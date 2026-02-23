package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.server.repositories.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Handles Event creation.
 */
public class EventsHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(EventsHandler.class);
    private final TicketRepository ticketRepository;

    public EventsHandler() {
        this.ticketRepository = new TicketRepository();
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            responseHeaders.add("Content-type", ContentType.JSON.toString());

            if (he.getRequestMethod().equals("POST")) {
                String response;
                try (InputStream is = he.getRequestBody()) {
                    Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name());
                    String requestBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";

                    JsonObject jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();

                    String name = jsonRequest.get("name").getAsString();
                    int totalTickets = jsonRequest.get("total_tickets").getAsInt();
                    String date = jsonRequest.get("date").getAsString();

                    ticketRepository.saveEvent(name, totalTickets, date);

                    logger.info("Evento creado: nombre={}, tickets={}, fecha={}", name, totalTickets, date);
                    Thread.sleep(3000);
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.addProperty("status", "OK");
                    jsonResponse.addProperty("message", "Event created successfully");
                    response = jsonResponse.toString();

                    he.sendResponseHeaders(200, response.length());
                } catch (Exception e) {
                    logger.error("Error al crear evento", e);
                    response = "{\"status\": \"NOK\",\"message\": \"Error creating event: " + e.getMessage() + "\"}";
                    he.sendResponseHeaders(500, response.length());
                }
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            if (he.getRequestMethod().equals("GET")) {
                String response;
                try {
                    String eventsJson = ticketRepository.getAllEvents().toString();
                    logger.debug("Eventos consultados: {} bytes", eventsJson.length());
                    byte[] byteResponse = eventsJson.getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(200, byteResponse.length);
                    OutputStream os = he.getResponseBody();
                    os.write(byteResponse);
                    os.close();
                } catch (Exception e) {
                    logger.error("Error al leer eventos", e);
                    response = "{\"status\": \"NOK\",\"message\": \"Error reading events: " + e.getMessage() + "\"}";
                    he.sendResponseHeaders(500, response.length());
                    OutputStream os = he.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
                return;
            }

            // Options response for CORS
            if (he.getRequestMethod().equals("OPTIONS")) {
                he.sendResponseHeaders(200, -1);
            } else {
                logger.warn("Método no soportado: {}", he.getRequestMethod());
                String response = "{\"status\": \"NOK\",\"message\": \"Method not supported\"}";
                he.sendResponseHeaders(405, response.length());
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } catch (Exception e) {
            logger.error("Error inesperado en EventsHandler", e);
        }
    }
}
