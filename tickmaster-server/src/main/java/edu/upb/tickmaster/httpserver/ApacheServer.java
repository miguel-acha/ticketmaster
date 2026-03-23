/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 *
 * @author rlaredo
 * 
 */
public class ApacheServer {

    private static final Logger logger = LoggerFactory.getLogger(ApacheServer.class);
    private HttpServer server = null;
    private boolean isServerDone = false;
    private final int PUERTO = 1914;

    public ApacheServer() {
    }

    public boolean start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
            // Registro de los diferentes servicios de la API
            AuthFilter authFilter = new AuthFilter();
            LoggingFilter loggingFilter = new LoggingFilter();

            this.server.createContext("/", exchange -> {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                new RootHandler().handle(exchange);
            }).getFilters().add(loggingFilter);

            this.server.createContext("/hola", new EchoPostHandler()).getFilters().add(loggingFilter);
            this.server.createContext("/eventos", new EventsHandler()).getFilters()
                    .addAll(java.util.List.of(loggingFilter, authFilter));
            this.server.createContext("/tickets", new TicketsHandler()).getFilters()
                    .addAll(java.util.List.of(loggingFilter, authFilter));
            this.server.createContext("/compras", new ComprasHandler()).getFilters()
                    .addAll(java.util.List.of(loggingFilter, authFilter));
            this.server.createContext("/webhook/pago", new WebhookHandler()).getFilters()
                    .add(loggingFilter);
            this.server.createContext("/health", new HealthCheckHandler()).getFilters().add(loggingFilter);
            this.server.createContext("/usuarios", new UsuariosHandler()).getFilters().add(loggingFilter);

            this.server.setExecutor(Executors.newFixedThreadPool(20));
            this.server.start();

            registrarMe();

            return true;
        } catch (IOException e) {
            logger.error("Error al iniciar el servidor HTTP", e);
            this.server = null;
        }
        return false;
    }

    private void registrarMe() {
        int maxRetries = 5;
        int delay = 2000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String lbUrl = System.getenv("LB_URL");
                if (lbUrl == null)
                    lbUrl = "http://localhost:1915";

                String serverName = System.getenv("SERVER_NAME");
                if (serverName == null || serverName.isEmpty() || serverName.equals("server")) {
                    try {
                        serverName = java.net.InetAddress.getLocalHost().getHostAddress();
                    } catch (Exception ex) {
                        serverName = "localhost";
                    }
                }

                String serviceName = System.getenv("SERVICE_NAME");
                if (serviceName == null)
                    serviceName = "servidor";

                URL url = new URL(lbUrl + "/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String ipAddress = InetAddress.getLocalHost().getHostAddress();
                String urlString = "http://" + ipAddress + ":" + PUERTO + "/";

                String json = String.format("{\"url\": \"%s\", \"serviceName\": \"%s\"}", urlString, serviceName);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() == 200) {
                    logger.info("Servidor registrado en {} con URL: {}", lbUrl, urlString);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Intento {} de registro fallido en el balanceador: {}", i + 1, e.getMessage());
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.error("No se pudo registrar el servidor tras {} intentos", maxRetries);
    }

    public void stop() {
        logger.info("Deteniendo servidor HTTP");
        this.server.stop(0);
        this.server = null;
    }

}
