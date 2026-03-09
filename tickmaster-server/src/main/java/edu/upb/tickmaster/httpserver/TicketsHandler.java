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
import java.util.Scanner;
import java.util.UUID;

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

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            // --- Idempotencia ---
            String idempotencyKey = json.has("idempotency_key")
                    ? json.get("idempotency_key").getAsString()
                    : null;

            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                JsonObject cached = ticketRepository.findByIdempotencyKey(idempotencyKey);
                if (cached != null) {
                    logger.info("Idempotencia: key '{}' ya procesada.", idempotencyKey);
                    enviarJson(he, 200, cached.toString());
                    return;
                }
            }

            // --- Datos del ticket ---
            int idEvento = json.get("id_evento").getAsInt();
            int idUsuario = json.get("id_usuario").getAsInt();
            String nroAsiento = json.has("nro_asiento") ? json.get("nro_asiento").getAsString() : "General";
            int idTipoTicket = json.get("id_tipo_ticket").getAsInt();

            // Obtener precio del tipo de ticket
            JsonObject tipoTicket = tipoTicketRepository.findById(idTipoTicket);
            if (tipoTicket == null) {
                enviarJson(he, 400, "{\"status\":\"NOK\",\"message\":\"Tipo de ticket no encontrado\"}");
                return;
            }
            double precio = tipoTicket.get("precio").getAsDouble();

            // Verificar y decrementar disponibilidad
            tipoTicketRepository.decrementarDisponibilidad(idTipoTicket);

            // Guardar el ticket
            String ticketId = UUID.randomUUID().toString();
            ticketRepository.saveTicket(ticketId, idEvento, idUsuario, nroAsiento, precio, idempotencyKey,
                    idTipoTicket);

            // Registrar la compra
            int idCompra = compraRepository.registrarCompra(ticketId, idUsuario, precio);

            logger.info("Ticket comprado: id={}, evento={}, usuario={}, compra={}", ticketId, idEvento, idUsuario,
                    idCompra);

            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("ticket_id", ticketId);
            res.addProperty("id_compra", idCompra);
            res.addProperty("precio", precio);
            res.addProperty("message", "Ticket comprado exitosamente");
            response = res.toString();
            enviarJson(he, 200, response);

        } catch (Exception e) {
            logger.error("Error al comprar ticket", e);
            response = "{\"status\":\"NOK\",\"message\":\"Error al comprar ticket: " + e.getMessage() + "\"}";
            enviarJson(he, 500, response);
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
