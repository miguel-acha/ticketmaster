/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
 * Proxy handler que reenvía peticiones GET y POST al servidor backend
 * 
 * @author rlaredo
 */
public class ProxyHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final String backendServerUrl;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 5000;

    public ProxyHandler(String backendServerUrl) {
        this.backendServerUrl = backendServerUrl;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String targetUrl = backendServerUrl + exchange.getRequestURI().toString();
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().toString();

            logger.info("Iniciando petición a: {}", targetUrl);

            // Crear conexión al servidor backend
            URL url = new URL(targetUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // Copiar headers de la petición original al backend
            Headers requestHeaders = exchange.getRequestHeaders();
            for (Map.Entry<String, List<String>> header : requestHeaders.entrySet()) {
                if (!header.getKey().equalsIgnoreCase("Host")) {
                    for (String value : header.getValue()) {
                        connection.setRequestProperty(header.getKey(), value);
                    }
                }
            }

            // Si es POST, copiar el body
            if (method.equals("POST") || method.equals("PUT")) {
                connection.setDoOutput(true);
                InputStream requestBody = exchange.getRequestBody();
                OutputStream backendOutput = connection.getOutputStream();

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = requestBody.read(buffer)) != -1) {
                    backendOutput.write(buffer, 0, bytesRead);
                }
                backendOutput.flush();
                backendOutput.close();
            }

            // Obtener respuesta del backend
            int responseCode = connection.getResponseCode();
            logger.info("Respuesta recibida del backend: status={}", responseCode);

            // Copiar headers de la respuesta del backend al cliente
            Headers responseHeaders = exchange.getResponseHeaders();
            Map<String, List<String>> backendHeaders = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> header : backendHeaders.entrySet()) {
                if (header.getKey() != null) {
                    for (String value : header.getValue()) {
                        responseHeaders.add(header.getKey(), value);
                    }
                }
            }

            // Leer el body de la respuesta del backend
            InputStream backendResponse;
            try {
                backendResponse = connection.getInputStream();
            } catch (IOException e) {
                backendResponse = connection.getErrorStream();
            }

            ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
            if (backendResponse != null) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = backendResponse.read(buffer)) != -1) {
                    responseBody.write(buffer, 0, bytesRead);
                }
                backendResponse.close();
            }

            byte[] responseBytes = responseBody.toByteArray();

            // Enviar respuesta al cliente
            exchange.sendResponseHeaders(responseCode, responseBytes.length);
            OutputStream clientOutput = exchange.getResponseBody();
            clientOutput.write(responseBytes);
            clientOutput.close();

            logger.info("Respuesta enviada: {} ({} bytes)", responseCode, responseBytes.length);

        } catch (IOException e) {
            logger.error("Error comunicado con el backend: {}", e.getMessage());
            exchange.sendResponseHeaders(500, -1);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            logger.error("Error inesperado: {}", e.getMessage());
            exchange.sendResponseHeaders(500, -1);
            exchange.getResponseBody().close();
        }
    }
}
