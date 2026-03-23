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
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    private static final AtomicInteger ACTIVE_CONNECTIONS = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 5;
    private static final boolean CONLIMIT_ENABLED = false; // Cambiar a false para desactivar
    public static String intentos = System.getenv("INTENTOS") != null ? System.getenv("INTENTOS") : "3";

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
        int current = ACTIVE_CONNECTIONS.incrementAndGet();
        try {
            if (CONLIMIT_ENABLED && current > MAX_CONNECTIONS) {
                logger.warn("Límite de conexiones alcanzado ({} > {}). Rechazando petición.", current, MAX_CONNECTIONS);
                exchange.sendResponseHeaders(503, -1); // Service Unavailable
                exchange.close();
                return;
            }

            String path = exchange.getRequestURI().getPath();

            // Manejar Registro Dinámico
            String normalizedPath = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
            if (normalizedPath.equalsIgnoreCase("/register")) {
                if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    handleRegistration(exchange);
                } else {
                    logger.warn("Petición {} a /register no permitida", exchange.getRequestMethod());
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    exchange.close();
                }
                return;
            }

            // Configurar CORS
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Access-Control-Allow-Origin", "*");
            responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
            responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Integrity-Check");

            // Manejar OPTIONS
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            // Extraer Path y Query
            String query = exchange.getRequestURI().getQuery();
            String fullPath = exchange.getRequestURI().getPath();

            String serviceName = "servidor";
            String pathForBackend = fullPath;

            String[] pathParts = fullPath.split("/", 3);
            if (pathParts.length > 1 && !pathParts[1].isEmpty()) {
                String potentialService = pathParts[1];
                if (ServerRegistro.getInstance().getAll().containsKey(potentialService)) {
                    serviceName = potentialService;
                    pathForBackend = "/" + (pathParts.length > 2 ? pathParts[2] : "");
                }
            }

            String backendServerUrl = ServerRegistro.getInstance().getNextServer(serviceName);

            if (backendServerUrl == null) {
                logger.warn("No hay servidores disponibles en el grupo: {}", serviceName);
                String response = "No hay ningún servidor disponible en la lista.";
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(503, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                exchange.close();
                return;
            }

            // Construir URL destino preservando el path sin el service name
            String targetUrl = backendServerUrl + pathForBackend + (query != null ? "?" + query : "");

            forwardRequest(exchange, targetUrl, serviceName, pathForBackend);
        } finally {
            ACTIVE_CONNECTIONS.decrementAndGet();
        }
    }

    private void handleRegistration(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String serviceName = json.has("serviceName") ? json.get("serviceName").getAsString() : "servidor";

            if (!json.has("url")) {
                throw new IllegalArgumentException("El campo 'url' es obligatorio para el registro.");
            }

            String urlString = json.get("url").getAsString();

            if (urlString.endsWith("/")) {
                urlString = urlString.substring(0, urlString.length() - 1);
            }

            ServerRegistro.getInstance().add(serviceName, urlString);

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

    private void forwardRequest(HttpExchange exchange, String targetServerUrl, String serviceName, String pathForBackend) throws IOException {
        String method = exchange.getRequestMethod();
        byte[] cachedRequestBody = new byte[0];
        if (method.equals("POST") || method.equals("PUT")) {
            try (InputStream is = exchange.getRequestBody()) {
                cachedRequestBody = is.readAllBytes();
            }
        }


        String currentTargetBaseUrl = targetServerUrl;
        String currentTargetUrl = targetServerUrl;
        
        for (int intento = 1; intento <= Integer.parseInt(ServerRegistro.intentos); intento++) {
            try {
                logger.info("Proxying a: {} (intento {})", currentTargetUrl, intento);
                URL url = new URL(currentTargetUrl);
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
                
                // Reportar el fallo usando el serviceName extraido
                ServerRegistro.getInstance().reportFailure(serviceName, currentTargetBaseUrl);

                if (intento < Integer.parseInt(ServerRegistro.intentos)) {
                    // Intentar obtener OTRO servidor para el siguiente intento
                    String nextBackendBaseUrl = ServerRegistro.getInstance().getNextServer(serviceName);
                    if (nextBackendBaseUrl != null) {
                        currentTargetBaseUrl = nextBackendBaseUrl;
                        String query = exchange.getRequestURI().getQuery();
                        currentTargetUrl = currentTargetBaseUrl + pathForBackend + (query != null ? "?" + query : "");
                        logger.info("Reintentando con nuevo servidor: {}", currentTargetUrl);
                    } else {
                        logger.warn("No hay más servidores disponibles para reintentar.");
                        String response = "No hay servidores activos para completar la petición.";
                        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(503, responseBytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(responseBytes);
                        }
                        exchange.close();
                        return;
                    }
                } else {
                    // Si ya agotamos los intentos
                    String response = "Se agotaron los intentos de conexión (" + intento + ") con los servidores disponibles.";
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(502, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                    exchange.close();
                }
            }
        }
    }
}
