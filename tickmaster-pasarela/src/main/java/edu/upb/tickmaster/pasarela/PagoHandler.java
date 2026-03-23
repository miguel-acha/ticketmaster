package edu.upb.tickmaster.pasarela;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PagoHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(PagoHandler.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    @Override
    public void handle(HttpExchange he) throws IOException {
        String method = he.getRequestMethod();
        if (method.equals("OPTIONS")) {
            he.sendResponseHeaders(200, -1);
            return;
        }

        if (!method.equals("POST")) {
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
            String idUsuario = json.get("id_usuario").getAsString();
            double monto = json.get("monto").getAsDouble();
            String ordenId = json.has("orden_id") ? json.get("orden_id").getAsString() : "ORD-UNKNOWN";
            String callbackUrl = json.has("callback_url") ? json.get("callback_url").getAsString() : null;
            String transactionId = "TXN-" + System.currentTimeMillis();

            logger.info("Recibida peticion de cobro: Usuario={}, Monto={}, OrdenId={}, TransactionId={}", idUsuario, monto, ordenId, transactionId);

            // Responder inmediatamente: 202 Accepted (Patrón Asíncrono)
            JsonObject res = new JsonObject();
            res.addProperty("status", "PENDING");
            res.addProperty("transaction_id", transactionId);
            res.addProperty("message", "Pago en proceso, se notificará via webhook");
            
            enviarRespuesta(he, 202, res.toString());

            // Simular procesamiento y enviar Webhook después de un delay
            if (callbackUrl != null) {
                scheduler.schedule(() -> enviarWebhook(callbackUrl, transactionId, idUsuario, monto, ordenId), 3, TimeUnit.SECONDS);
            } else {
                logger.warn("No se proporcionó callback_url para la transacción {}", transactionId);
            }

        } catch (Exception e) {
            logger.error("Error al procesar cobro", e);
            enviarRespuesta(he, 400, "{\"status\":\"NOK\",\"message\":\"Error en el formato de la peticion\"}");
        }
    }

    private void enviarWebhook(String callbackUrl, String transactionId, String idUsuario, double monto, String ordenId) {
        logger.info("Enviando webhook a {} para transaccion {}", callbackUrl, transactionId);
        try {
            JsonObject webhookData = new JsonObject();
            webhookData.addProperty("transaction_id", transactionId);
            webhookData.addProperty("id_usuario", idUsuario);
            webhookData.addProperty("orden_id", ordenId);
            webhookData.addProperty("monto", monto);
            webhookData.addProperty("status", "COMPLETED");
            webhookData.addProperty("timestamp", System.currentTimeMillis());

            URL url = new URL(callbackUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(webhookData.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            logger.info("Webhook enviado exitosamente. Respuesta del servidor: {}", responseCode);
        } catch (Exception e) {
            logger.error("Error al enviar webhook a {}: {}", callbackUrl, e.getMessage());
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
