package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.server.repositories.CompraRepository;
import edu.upb.tickmaster.server.repositories.TicketRepository;
import edu.upb.tickmaster.server.repositories.TipoTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Scanner;
import java.util.UUID;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonArray;
import edu.upb.tickmaster.db.ConexionDb;

/**
 * Maneja la compra de tickets.
 *
 * POST /tickets → comprar ticket
 * Body: { id_evento, id_usuario, nro_asiento, id_tipo_ticket, idempotency_key }
 *
 * GET /tickets?usuario=N → listar tickets de un usuario
 * GET /tickets → listar todos los tickets (admin)
 */
public class TicketsHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(TicketsHandler.class);
    private final TicketRepository ticketRepository;
    private final TipoTicketRepository tipoTicketRepository;
    private final CompraRepository compraRepository;

    public TicketsHandler() {
        this.ticketRepository = new TicketRepository();
        this.tipoTicketRepository = new TipoTicketRepository();
        this.compraRepository = new CompraRepository();
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.add("Content-type", ContentType.JSON.toString());

            String method = he.getRequestMethod();

            if (method.equals("OPTIONS")) {
                he.sendResponseHeaders(200, -1);
                return;
            }

            if (method.equals("POST")) {
                handleComprarTicket(he);
            } else if (method.equals("GET")) {
                handleListarTickets(he);
            } else {
                enviarJson(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
            }

        } catch (Exception e) {
            logger.error("Error inesperado en TicketsHandler", e);
        }
    }

    // POST /tickets
    private void handleComprarTicket(HttpExchange he) throws IOException {
        String response;
        Connection conn = null;
        try {
            String body;
            byte[] cachedBody = (byte[]) he.getAttribute("cached_body");

            if (cachedBody != null) {
                body = new String(cachedBody, StandardCharsets.UTF_8);
            } else {
                try (InputStream is = he.getRequestBody();
                        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                    body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                }
            }
            logger.info("[TRACE] Cuerpo de petición recibido ({} bytes). Iniciando validación...", body.length());

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            int idUsuario = json.get("id_usuario").getAsInt();
            JsonArray carrito = json.getAsJsonArray("carrito");
            String ordenId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            logger.info("[TRACE] Iniciando procesamiento de orden {} para usuario {}", ordenId, idUsuario);
            logger.info("[TRACE] Analizando carrito: Usuario={}, Orden={}, Items={}", idUsuario, ordenId, (carrito != null ? carrito.size() : 0));

            if (carrito == null || carrito.size() == 0) {
                logger.warn("[TRACE] Intento de compra con carrito vacío.");
                enviarJson(he, 400, "{\"status\":\"NOK\",\"message\":\"El carrito esta vacio\"}");
                return;
            }

            conn = ConexionDb.getInstance().getConnection();
            // conn.setAutoCommit(false); // Iniciar transaccion (bloqueo pesimista)

            double montoTotal = 0;
            java.util.List<JsonObject> ticketsAComprar = new java.util.ArrayList<>();

            // 1. FASE DE RESERVA (TRANSMISIÓN DB RÁPIDA)
            synchronized (conn) {
                boolean originalAutoCommit = conn.getAutoCommit();
                try {
                    conn.setAutoCommit(false);
                    for (int i = 0; i < carrito.size(); i++) {
                        JsonObject item = carrito.get(i).getAsJsonObject();
                        int idTipoTicket = item.get("id_tipo_ticket").getAsInt();
                        int cantidad = item.get("cantidad").getAsInt();

                        JsonObject tipoTicket = tipoTicketRepository.findById(idTipoTicket);
                        if (tipoTicket == null)
                            throw new Exception("Tipo de ticket no encontrado: " + idTipoTicket);

                        int idEventoReal = tipoTicket.get("id_evento").getAsInt();
                        double precio = tipoTicket.get("precio").getAsDouble();
                        logger.info("[TRACE] Reservando stock: ID={}, Cantidad={}, Precio={}", idTipoTicket, cantidad, precio);

                        logger.info("Reserva Stock (Pesimista): TicketType={}, Cantidad={}", idTipoTicket, cantidad);
                        tipoTicketRepository.decrementarDisponibilidadConBloqueo(conn, idTipoTicket, cantidad);
                        logger.info("[TRACE] Stock decrementado en DB.");

                        montoTotal += (precio * cantidad);
                        for (int c = 0; c < cantidad; c++) {
                            JsonObject tItem = new JsonObject();
                            tItem.addProperty("id_evento", idEventoReal);
                            tItem.addProperty("id_tipo_ticket", idTipoTicket);
                            tItem.addProperty("precio", precio);
                            ticketsAComprar.add(tItem);
                        }
                    }
                    logger.info("[TRACE] Fase de reserva completada. Committing stock...");
                    conn.commit(); // Soltamos el stock reservado para este hilo
                    logger.info("[TRACE] Commit de stock exitoso.");
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }
            }

            // 2. FASE DE PAGO (INICIO ASÍNCRONO)
            boolean peticionPasarelaOK = false;
            try {
                String pasarelaUrl = System.getenv("PASARELA_URL") != null ? System.getenv("PASARELA_URL") : "http://localhost:1916/cobrar";
                String callbackUrl = System.getenv("CALLBACK_URL") != null ? System.getenv("CALLBACK_URL") : "http://localhost:1914/webhook/pago";

                logger.info("[TRACE] Contactando pasarela: {} con monto: {}", pasarelaUrl, montoTotal);
                JsonObject pasarelaBody = new JsonObject();
                pasarelaBody.addProperty("id_usuario", idUsuario);
                pasarelaBody.addProperty("monto", montoTotal);
                pasarelaBody.addProperty("orden_id", ordenId);
                pasarelaBody.addProperty("callback_url", callbackUrl);

                URL url = new URL(pasarelaUrl);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("Content-Type", "application/json");
                httpConn.setConnectTimeout(5000);
                httpConn.setDoOutput(true);

                try (OutputStream os = httpConn.getOutputStream()) {
                    os.write(pasarelaBody.toString().getBytes(StandardCharsets.UTF_8));
                }
                
                int respCode = httpConn.getResponseCode();
                logger.info("[TRACE] Respuesta Pasarela: HTTP {}", respCode);
                peticionPasarelaOK = (respCode == 202 || respCode == 200);
                if (!peticionPasarelaOK) {
                    logger.warn("Pasarela (en {}) rechazó la petición: HTTP {}", pasarelaUrl, respCode);
                }
            } catch (Exception e) {
                String pasarelaUrl = System.getenv("PASARELA_URL") != null ? System.getenv("PASARELA_URL") : "http://localhost:1916/cobrar";
                logger.error("Error de red al conectar con la pasarela en {}: {}", pasarelaUrl, e.getMessage());
            }

            // 3. FASE DE FINALIZACIÓN INICIAL (PENDIENTE) O COMPENSACIÓN
            synchronized (conn) {
                boolean originalAutoCommit = conn.getAutoCommit();
                try {
                    conn.setAutoCommit(false);
                    if (peticionPasarelaOK) {
                        logger.info("[TRACE] Pasarela aceptó la orden. Registrando tickets como PENDIENTE...");
                        // Registrar tickets y compras pero con estado PENDIENTE
                        JsonArray purchasedTicketIds = new JsonArray();
                        for (JsonObject tk : ticketsAComprar) {
                            String ticketId = UUID.randomUUID().toString();
                            String ticketIdempotencyKey = "TKM-" + UUID.randomUUID().toString();
                            ticketRepository.saveTicket(ticketId, tk.get("id_evento").getAsInt(), idUsuario, "General",
                                    tk.get("precio").getAsDouble(), ticketIdempotencyKey,
                                    tk.get("id_tipo_ticket").getAsInt());
                            
                            // Registramos la compra como 'pendiente'
                            compraRepository.registrarCompra(ticketId, idUsuario, tk.get("precio").getAsDouble(),
                                    ordenId, "pendiente");
                            purchasedTicketIds.add(ticketId);
                        }
                        logger.info("[TRACE] {} tickets registrados en DB. Confirmando transacción...", ticketsAComprar.size());
                        conn.commit();
                        logger.info("[TRACE] Transacción finalizada. Webhook esperado.");
                        logger.info("Pedido registrado como PENDIENTE. Esperando webhook...");
                        JsonObject res = new JsonObject();
                        res.addProperty("status", "PENDING");
                        res.add("tickets_ids", purchasedTicketIds);
                        res.addProperty("orden_id", ordenId);
                        res.addProperty("message", "Pago en proceso. Sus tickets están reservados.");
                        enviarJson(he, 202, res.toString());
                    } else {
                        // COMPENSACIÓN INMEDIATA: Devolver el stock si la pasarela ni siquiera respondió
                        logger.warn("Revirtiendo stock porque la pasarela no está disponible...");
                        for (int i = 0; i < carrito.size(); i++) {
                            JsonObject item = carrito.get(i).getAsJsonObject();
                            int idTipoTicket = item.get("id_tipo_ticket").getAsInt();
                            int cantidad = item.get("cantidad").getAsInt();
                            String sqlComp = "UPDATE tipo_ticket SET cantidad = cantidad + ? WHERE id_tipo_ticket = ?";
                            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlComp)) {
                                pstmt.setInt(1, cantidad);
                                pstmt.setInt(2, idTipoTicket);
                                pstmt.executeUpdate();
                            }
                        }
                        conn.commit();
                        enviarJson(he, 503, "{\"status\":\"NOK\",\"message\":\"Pasarela de pago no disponible, intente más tarde\"}");
                    }
                } catch (Exception e) {
                    conn.rollback();
                    logger.error("Error en fase final: {}", e.getMessage());
                    throw e;
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }
            }

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                }
            }
            logger.error("Error al comprar carrito", e);
            response = "{\"status\":\"NOK\",\"message\":\"Error al comprar tickets: " + e.getMessage() + "\"}";
            enviarJson(he, 500, response);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ex) {
                }
            }
        }
    }

    // GET /tickets o GET /tickets?usuario=N
    private void handleListarTickets(HttpExchange he) throws IOException {
        try {
            String query = he.getRequestURI().getQuery();
            String response;

            if (query != null && query.startsWith("usuario=")) {
                int idUsuario = Integer.parseInt(query.substring(8));
                response = ticketRepository.listarPorUsuario(idUsuario).toString();
            } else {
                response = ticketRepository.listarTodos().toString();
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            logger.error("Error al listar tickets", e);
            enviarJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al listar tickets\"}");
        }
    }

    private void enviarJson(HttpExchange he, int code, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }
}
