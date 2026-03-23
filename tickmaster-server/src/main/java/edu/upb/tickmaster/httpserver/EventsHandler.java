package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.server.repositories.EventoRepository;
import edu.upb.tickmaster.server.repositories.TipoTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Maneja operaciones sobre Eventos y Tipos de Ticket.
 *
 * POST /eventos → crear evento (body: {nombre, fecha, capacidad})
 * GET /eventos → listar todos los eventos
 * POST /eventos/tipos → crear tipo de ticket (body: {id_evento, tipo_asiento,
 * cantidad, precio})
 * GET /eventos/tipos?id=N → listar tipos de ticket de un evento
 */
public class EventsHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(EventsHandler.class);
    private final EventoRepository eventoRepository;
    private final TipoTicketRepository tipoTicketRepository;

    public EventsHandler() {
        this.eventoRepository = new EventoRepository();
        this.tipoTicketRepository = new TipoTicketRepository();
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.add("Content-type", ContentType.JSON.toString());

            String path = he.getRequestURI().getPath();
            String method = he.getRequestMethod();

            if (method.equals("OPTIONS")) {
                he.sendResponseHeaders(200, -1);
                return;
            }

            // --- Tipos de ticket (Listar) ---
            if (path.endsWith("/tipos")) {
                if (method.equals("GET")) {
                    handleListarTiposTicket(he);
                } else {
                    enviarJson(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
                }
                return;
            }

            // --- Eventos ---
            if (method.equals("POST")) {
                handleCrearEvento(he);
            } else if (method.equals("GET")) {
                handleListarEventos(he);
            } else if (method.equals("DELETE")) {
                handleEliminarEvento(he);
            } else {
                enviarJson(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
            }

        } catch (Exception e) {
            logger.error("Error inesperado en EventsHandler", e);
        }
    }

    // POST /eventos
    private void handleCrearEvento(HttpExchange he) throws IOException {
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

            String nombre = (json.has("nombre") && !json.get("nombre").isJsonNull()) ? json.get("nombre").getAsString() : "";
            String fecha = (json.has("fecha") && !json.get("fecha").isJsonNull()) ? json.get("fecha").getAsString() : "";
            String imagenUrl = (json.has("imagen_url") && !json.get("imagen_url").isJsonNull()) ? json.get("imagen_url").getAsString() : null;

            JsonArray tiposTickets = null;
            if (json.has("tipos_tickets") && !json.get("tipos_tickets").isJsonNull()) {
                tiposTickets = json.getAsJsonArray("tipos_tickets");
            }

            int id = eventoRepository.crearEvento(nombre, fecha, imagenUrl, tiposTickets);

            logger.info("Evento creado: nombre={}, id={}, con {} tipos de tickets", nombre, id, (tiposTickets != null ? tiposTickets.size() : 0));

            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("id_evento", id);
            res.addProperty("message", "Evento creado exitosamente");
            enviarJson(he, 200, res.toString());

        } catch (Exception e) {
            logger.error("Error al crear evento", e);
            enviarJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al crear evento: " + e.getMessage() + "\"}");
        }
    }

    // GET /eventos
    private void handleListarEventos(HttpExchange he) throws IOException {
        try {
            String response = eventoRepository.listarEventos().toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            logger.error("Error al listar eventos", e);
            enviarJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al listar eventos\"}");
        }
    }

    // CREAR TIPO TICKET AISLADO ELIMINADO

    // GET /eventos/tipos?id=N
    private void handleListarTiposTicket(HttpExchange he) throws IOException {
        try {
            String query = he.getRequestURI().getQuery();
            int idEvento = 0;
            if (query != null && query.startsWith("id=")) {
                idEvento = Integer.parseInt(query.substring(3));
            }

            String response = tipoTicketRepository.listarPorEvento(idEvento).toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            logger.error("Error al listar tipos de ticket", e);
            enviarJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al listar tipos de ticket\"}");
        }
    }

    // DELETE /eventos?id=N
    private void handleEliminarEvento(HttpExchange he) throws IOException {
        try {
            String query = he.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                int idEvento = Integer.parseInt(query.substring(3));
                eventoRepository.eliminarEvento(idEvento);
                logger.info("Evento eliminado: id={}", idEvento);
                enviarJson(he, 200, "{\"status\":\"OK\",\"message\":\"Evento eliminado correctamente\"}");
            } else {
                enviarJson(he, 400, "{\"status\":\"NOK\",\"message\":\"Falta parametro id\"}");
            }
        } catch (Exception e) {
            logger.error("Error al eliminar evento", e);
            enviarJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al eliminar evento: " + e.getMessage() + "\"}");
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
