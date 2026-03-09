package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ProxyHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    static {
        loadInitialRoutes();
    }

    private static void loadInitialRoutes() {
        try (InputStream input = ProxyHandler.class.getClassLoader().getResourceAsStream("routes.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                for (String serviceName : props.stringPropertyNames()) {
                    String urls = props.getProperty(serviceName);
                    for (String url : urls.split(",")) {

                        ServerRegistro.getInstance().add(serviceName, url.trim());
                    }
                }
                logger.info("Rutas iniciales cargadas desde properties");
            }
        } catch (IOException ex) {
            logger.error("Error cargando routes.properties", ex);
        }
    }

    public ProxyHandler() {
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Manejar Registro Dinámico
        if (path.equals("/registrar") && exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            handleRegistration(exchange);
            return;
        }

        // Configurar CORS
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        responseHeaders.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Manejar OPTIONS
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        // Extraer Prefijo (Isla de Servicio)
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String[] pathSegments = cleanPath.split("/", 2);

        if (pathSegments.length < 1 || pathSegments[0].isEmpty()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        String serviceName = pathSegments[0];
        String backendServerUrl = ServerRegistro.getInstance().getNextServer(serviceName);

        if (backendServerUrl == null) {
            logger.warn("No hay servidores disponibles para el servicio: {}", serviceName);
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
            return;
        }

        // Construir URL destino preservando el resto del path y query
        String remainingPath = (pathSegments.length > 1) ? "/" + pathSegments[1] : "";
        String query = exchange.getRequestURI().getQuery();
        String targetUrl = backendServerUrl + remainingPath + (query != null ? "?" + query : "");

        forwardRequest(exchange, targetUrl);
    }

    private void handleRegistration(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String serviceName = json.has("serviceName") ? json.get("serviceName").getAsString() : "servidor";
            String ip = json.get("ip").getAsString();
            int puerto = json.get("puerto").getAsInt();

            ServerRegistro.getInstance().add(serviceName, ip, puerto);

            String response = "{\"status\": \"registrado\", \"servicio\": \"" + serviceName + "\"}";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            logger.error("Error en registro: {}", e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        } finally {
            exchange.close();
        }
    }

    private void forwardRequest(HttpExchange exchange, String targetUrl) throws IOException {
        String method = exchange.getRequestMethod();
        byte[] cachedRequestBody = new byte[0];
        if (method.equals("POST") || method.equals("PUT")) {
            try (InputStream is = exchange.getRequestBody()) {
                cachedRequestBody = is.readAllBytes();
            }
        }

        int intento = 0;
        while (intento < 2) {
            intento++;
            try {
                logger.info("Proxying a: {} (intento {})", targetUrl, intento);
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);

                // Copiar cabeceras del cliente al backend
                Headers clientHeaders = exchange.getRequestHeaders();
                for (Map.Entry<String, List<String>> header : clientHeaders.entrySet()) {
                    if (!header.getKey().equalsIgnoreCase("Host")) {
                        for (String value : header.getValue()) {
                            conn.setRequestProperty(header.getKey(), value);
                        }
                    }
                }

                if (method.equals("POST") || method.equals("PUT")) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(cachedRequestBody);
                    }
                }

                int responseCode = conn.getResponseCode();

                // Copiar cabeceras de respuesta al cliente
                Headers proxyResponseHeaders = exchange.getResponseHeaders();
                for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                    String key = entry.getKey();
                    if (key != null && !key.equalsIgnoreCase("Transfer-Encoding")
                            && !key.equalsIgnoreCase("Access-Control-Allow-Origin")) {
                        for (String value : entry.getValue()) {
                            proxyResponseHeaders.add(key, value);
                        }
                    }
                }

                InputStream is = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                byte[] responseBytes = (is != null) ? is.readAllBytes() : new byte[0];
                exchange.sendResponseHeaders(responseCode, responseBytes.length == 0 ? -1 : responseBytes.length);

                if (responseBytes.length > 0) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } else {
                    exchange.getResponseBody().close();
                }
                return;
            } catch (IOException e) {
                logger.error("Error proxying (intento {}): {}", intento, e.getMessage());
                if (intento >= 2) {
                    exchange.sendResponseHeaders(502, -1);
                    exchange.close();
                }
            }
        }
    }
}
