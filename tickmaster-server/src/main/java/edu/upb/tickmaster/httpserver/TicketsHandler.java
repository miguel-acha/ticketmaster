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
import java.util.UUID;

/**
 * Handles Ticket purchases.
 */
public class TicketsHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(TicketsHandler.class);
    private final TicketRepository ticketRepository;

    public TicketsHandler() {
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
                try (InputStream is = he.getRequestBody();
                        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                    String requestBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";

                    JsonObject jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();

                    String idempotencyKey = jsonRequest.has("idempotency_key")
                            ? jsonRequest.get("idempotency_key").getAsString()
                            : null;

                    if (idempotencyKey != null) {
                        JsonObject cachedResponse = ticketRepository.findByIdempotencyKey(idempotencyKey);
                        if (cachedResponse != null) {
                            logger.info("Idempotency match found for key: {}. Returning cached response.",
                                    idempotencyKey);
                            response = cachedResponse.toString();
                            he.sendResponseHeaders(200, response.length());
                            OutputStream os = he.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                            return;
                        }
                    }

                    int eventId = jsonRequest.get("event_id").getAsInt();
                    String userName = jsonRequest.get("user_name").getAsString();
                    String seatNumber = jsonRequest.has("seat_number") ? jsonRequest.get("seat_number").getAsString()
                            : "Any";
                    String ticketId = UUID.randomUUID().toString();

                    ticketRepository.saveTicket(ticketId, eventId, userName, seatNumber, idempotencyKey);
                    ticketRepository.incrementSoldTickets(eventId);

                    logger.info("Ticket comprado: id={}, evento={}, usuario={}", ticketId, eventId, userName);

                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.addProperty("status", "OK");
                    jsonResponse.addProperty("ticket_id", ticketId);
                    jsonResponse.addProperty("message", "Ticket purchased successfully");
                    response = jsonResponse.toString();

                    he.sendResponseHeaders(200, response.length());
                } catch (Exception e) {
                    logger.error("Error al comprar ticket", e);
                    response = "{\"status\": \"NOK\",\"message\": \"Error buying ticket: " + e.getMessage() + "\"}";
                    he.sendResponseHeaders(500, response.length());
                }
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

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
            logger.error("Error inesperado en TicketsHandler", e);
        }
    }
}
