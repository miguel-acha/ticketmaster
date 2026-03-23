package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.db.ConexionDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Scanner;

public class WebhookHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebhookHandler.class);

    @Override
    public void handle(HttpExchange he) throws IOException {
        if (!he.getRequestMethod().equals("POST")) {
            enviarRespuesta(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
            return;
        }

        try {
            String body;
            try (InputStream is = he.getRequestBody();
                 Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                body = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String ordenId = json.get("orden_id").getAsString();
            String status = json.get("status").getAsString();

            logger.info("Webhook recibido para Orden {}: Status={}", ordenId, status);

            if ("COMPLETED".equalsIgnoreCase(status)) {
                finalizarCompra(ordenId);
            } else {
                logger.warn("Recibido status no exitoso en webhook para orden {}: {}", ordenId, status);
                // Aquí se podría implementar lógica de cancelación/reversión
            }

            enviarRespuesta(he, 200, "{\"status\":\"OK\",\"message\":\"Webhook procesado\"}");

        } catch (Exception e) {
            logger.error("Error al procesar webhook", e);
            enviarRespuesta(he, 500, "{\"status\":\"NOK\",\"message\":\"Error interno\"}");
        }
    }

    private void finalizarCompra(String ordenId) throws Exception {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "UPDATE compras SET estado = 'completada' WHERE orden_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ordenId);
            int rows = pstmt.executeUpdate();
            logger.info("Compra {} marcada como completada. Filas afectadas: {}", ordenId, rows);
        }
    }

    private void enviarRespuesta(HttpExchange he, int code, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        he.getResponseHeaders().add("Content-Type", "application/json");
        he.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }
}
