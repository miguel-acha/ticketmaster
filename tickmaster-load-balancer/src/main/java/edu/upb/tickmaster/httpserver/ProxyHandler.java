package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Reenvía peticiones al backend con lógica de reintento.
 * Si el backend tarda más de READ_TIMEOUT ms (Read timed out), reintenta una
 * vez.
 * La idempotencia del backend evita duplicados en el reintento.
 *
 * 502 → Connect timed out (no se pudo conectar al backend)
 * 504 → Read timed out en ambos intentos (backend no respondió a tiempo)
 */
public class ProxyHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final String backendServerUrl;
    private static final int CONNECT_TIMEOUT = 10000; // 10s para conectar
    private static final int READ_TIMEOUT = 1995; // 1.995s para recibir respuesta

    public ProxyHandler(String backendServerUrl) {
        this.backendServerUrl = backendServerUrl;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String targetUrl = backendServerUrl + exchange.getRequestURI().toString();
        String method = exchange.getRequestMethod();

        // Leer el body UNA sola vez: el InputStream solo se puede leer una vez,
        // así que lo guardamos en memoria para poder reenviar en el reintento
        byte[] cachedRequestBody = new byte[0];
        if (method.equals("POST") || method.equals("PUT")) {
            try (InputStream is = exchange.getRequestBody()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                cachedRequestBody = baos.toByteArray();
            }
        }

        // Bucle de reintentos (máximo 2 intentos)
        int intento = 0;
        while (intento < 2) {
            intento++;
            try {
                logger.info("Petición a: {} (intento {})", targetUrl, intento);

                URL url = new URL(targetUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                // Pasar headers del cliente al backend (excepto Host)
                Headers requestHeaders = exchange.getRequestHeaders();
                for (Map.Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    if (!header.getKey().equalsIgnoreCase("Host")) {
                        for (String value : header.getValue()) {
                            connection.setRequestProperty(header.getKey(), value);
                        }
                    }
                }

                // Enviar body (POST/PUT) usando los datos cacheados
                if (method.equals("POST") || method.equals("PUT")) {
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    try (OutputStream backendOutput = connection.getOutputStream()) {
                        backendOutput.write(cachedRequestBody);
                        backendOutput.flush();
                    }
                } else {
                    connection.setDoInput(true);
                }

                int responseCode = connection.getResponseCode();

                // Copiar headers de respuesta del backend al cliente
                Headers responseHeaders = exchange.getResponseHeaders();
                Map<String, List<String>> backendHeaders = connection.getHeaderFields();
                for (Map.Entry<String, List<String>> header : backendHeaders.entrySet()) {
                    if (header.getKey() != null) {
                        for (String value : header.getValue()) {
                            responseHeaders.add(header.getKey(), value);
                        }
                    }
                }

                // Leer respuesta del backend (getErrorStream si código >= 400)
                InputStream backendResponse;
                try {
                    backendResponse = connection.getInputStream();
                } catch (IOException e) {
                    backendResponse = connection.getErrorStream();
                }

                ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
                if (backendResponse != null) {
                    try {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = backendResponse.read(buffer)) != -1) {
                            responseBody.write(buffer, 0, bytesRead);
                        }
                    } finally {
                        backendResponse.close();
                    }
                }

                // Enviar respuesta al cliente
                byte[] responseBytes = responseBody.toByteArray();
                exchange.sendResponseHeaders(responseCode, responseBytes.length);
                try (OutputStream clientOutput = exchange.getResponseBody()) {
                    clientOutput.write(responseBytes);
                }

                logger.info("Respuesta enviada: {}", responseCode);
                return; // Éxito

            } catch (IOException e) {
                String errorMsg = (e.getMessage() != null) ? e.getMessage() : "";

                // Read Timeout en el primer intento: el backend ya procesó la petición
                // pero no alcanzó a responder. "continue" dispara el segundo intento.
                // La idempotencia del backend devuelve 200 sin duplicar datos.
                if (errorMsg.contains("Read timed out") && intento == 1) {
                    logger.warn("Read timed out, reintentando una vez más...");
                    continue;
                }

                // Error final → 502 si no se conectó, 504 si no respondió a tiempo
                logger.error("Error comunicado con el backend: {}", errorMsg);
                if (errorMsg.contains("Connect timed out")) {
                    exchange.sendResponseHeaders(502, -1);
                } else {
                    exchange.sendResponseHeaders(504, -1);
                }
                exchange.getResponseBody().close();
                return;

            } catch (Exception e) {
                logger.error("Error inesperado: {}", e.getMessage());
                exchange.sendResponseHeaders(500, -1);
                exchange.getResponseBody().close();
                return;
            }
        }
    }
}
