package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler que procesa el registro de nuevos servidores backend.
 */
public class RegistroHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(RegistroHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        String ip = json.get("ip").getAsString();
        int puerto = json.get("puerto").getAsInt();

        ServerRegistro.add(ip, puerto);
        logger.info("Servidor registrado: {}:{}", ip, puerto);

        String response = "{\"status\": \"registrado\", \"servidores_activos\": " + ServerRegistro.getAll().size()
                + "}";
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
