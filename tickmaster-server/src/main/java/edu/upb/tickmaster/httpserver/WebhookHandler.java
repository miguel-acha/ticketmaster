package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Handler para recibir notificaciones (Webhooks) firmadas con HMAC.
 */
public class WebhookHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebhookHandler.class);
    // En produccion, esto deberia cargarse desde una variable de entorno o archivo
    // de config
    private static final String SHARED_SECRET = "pikachu8978";

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            // Permitir OPTIONS para CORS si es necesario
            if (he.getRequestMethod().equals("OPTIONS")) {
                he.sendResponseHeaders(204, -1);
                return;
            }

            if (!he.getRequestMethod().equals("POST")) {
                sendResponse(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no permitido, usar POST\"}");
                return;
            }

            // 1. Obtener firma del header
            Headers headers = he.getRequestHeaders();
            String receivedSignature = headers.getFirst("X-Signature");

            if (receivedSignature == null || receivedSignature.isEmpty()) {
                logger.warn("Peticion de webhook sin firma");
                sendResponse(he, 400, "{\"status\":\"NOK\",\"message\":\"X-Signature header es requerido\"}");
                return;
            }

            // 2. Leer body crudo para verificar firma
            byte[] bodyBytes = he.getRequestBody().readAllBytes();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);

            // 3. Calcular firma localmente
            String calculatedSignature = calculateHMAC(body, SHARED_SECRET);

            // 4. Comparar firmas
            if (verifySignature(receivedSignature, calculatedSignature)) {
                logger.info("Webhook VALIDADO: Body={}", body);

                // TODO: Implementar logica de negocio segun el evento recibido
                // Ejemplo: if(body.contains("PAID")) { ticketRepository.marcarPagado(...) }

                sendResponse(he, 200, "{\"status\":\"OK\",\"message\":\"Webhook recibido y validado\"}");
            } else {
                logger.error("Firma INVALIDA. Recibida: {}, Calculada: {}", receivedSignature, calculatedSignature);
                sendResponse(he, 401, "{\"status\":\"NOK\",\"message\":\"Firma invalida\"}");
            }

        } catch (

        Exception e) {
            logger.error("Error procesando webhook", e);
            sendResponse(he, 500, "{\"status\":\"NOK\",\"message\":\"Error interno del servidor\"}");
        }
    }

    private String calculateHMAC(String data, String secret) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(secretKey);
        byte[] hashBytes = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    private boolean verifySignature(String received, String calculated) {
        return MessageDigest.isEqual(received.getBytes(StandardCharsets.UTF_8),
                calculated.getBytes(StandardCharsets.UTF_8));
    }

    private void sendResponse(HttpExchange he, int code, String body) throws IOException {
        he.getResponseHeaders().set("Content-Type", "application/json");
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, response.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(response);
        }
    }
}
