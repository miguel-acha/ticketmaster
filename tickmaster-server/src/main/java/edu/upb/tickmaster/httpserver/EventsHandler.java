package edu.upb.tickmaster.httpserver;

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
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            responseHeaders.add("Content-type", ContentType.JSON.toString());

            String path = he.getRequestURI().getPath();
            String method = he.getRequestMethod();

            if (method.equals("OPTIONS")) {
                he.sendResponseHeaders(200, -1);
                return;
            }

            // --- Tipos de ticket ---
            if (path.endsWith("/tipos")) {
                if (method.equals("POST")) {
                    handleCrearTipoTicket(he);
                } else if (method.equals("GET")) {
                    handleListarTiposTicket(he);
                } else {
                    sendJson(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
                }
                return;
            }

            // --- Eventos ---
            if (method.equals("POST")) {
                handleCrearEvento(he);
            } else if (method.equals("GET")) {
                handleListarEventos(he);
            } else {
                sendJson(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
            }

        } catch (Exception e) {
            logger.error("Error inesperado en EventsHandler", e);
        }
    }

    // POST /eventos
    private void handleCrearEvento(HttpExchange he) throws IOException {
        try (InputStream is = he.getRequestBody();
                Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {

            String body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String nombre = json.get("nombre").getAsString();
            String fecha = json.get("fecha").getAsString();
            int capacidad = json.get("capacidad").getAsInt();

            int id = eventoRepository.crearEvento(nombre, fecha, capacidad);

            logger.info("Evento creado: nombre={}, capacidad={}, id={}", nombre, capacidad, id);

            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("id_evento", id);
            res.addProperty("message", "Evento creado exitosamente");
            sendJson(he, 200, res.toString());

        } catch (Exception e) {
            logger.error("Error al crear evento", e);
            sendJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al crear evento: " + e.getMessage() + "\"}");
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
            sendJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al listar eventos\"}");
        }
    }

    // POST /eventos/tipos
    private void handleCrearTipoTicket(HttpExchange he) throws IOException {
        try (InputStream is = he.getRequestBody();
                Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {

            String body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            int idEvento = json.get("id_evento").getAsInt();
            String tipoAsiento = json.get("tipo_asiento").getAsString();
            int cantidad = json.get("cantidad").getAsInt();
            double precio = json.get("precio").getAsDouble();

            int id = tipoTicketRepository.crear(idEvento, tipoAsiento, cantidad, precio);

            logger.info("Tipo de ticket creado: evento={}, tipo={}, id={}", idEvento, tipoAsiento, id);

            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("id_tipo_ticket", id);
            res.addProperty("message", "Tipo de ticket creado exitosamente");
            sendJson(he, 200, res.toString());

        } catch (Exception e) {
            logger.error("Error al crear tipo de ticket", e);
            sendJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error: " + e.getMessage() + "\"}");
        }
    }

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
            sendJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al listar tipos de ticket\"}");
        }
    }

    private void sendJson(HttpExchange he, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }
}
