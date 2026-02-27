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
            this.server.createContext("/", exchange -> {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
                new RootHandler().handle(exchange);
            });

            // Registro de los diferentes servicios de la API
            this.server.createContext("/hola", new EchoPostHandler());
            this.server.createContext("/eventos", new EventsHandler());
            this.server.createContext("/tickets", new TicketsHandler());
            this.server.createContext("/health", new HealthCheckHandler());

            this.server.setExecutor(Executors.newFixedThreadPool(2));
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
        try {
            String lbUrl = System.getenv("LB_URL");
            if (lbUrl == null)
                lbUrl = "http://localhost:1915";

            String serverName = System.getenv("SERVER_NAME");
            if (serverName == null)
                serverName = "localhost";

            URL url = new URL(lbUrl + "/registrar");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = "{\"ip\": \"" + serverName + "\", \"puerto\": " + PUERTO + "}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                logger.info("Servidor registrado con exito en el balanceador en {}", lbUrl);
            }
        } catch (Exception e) {
            logger.error("Error al registrarse en el balanceador: " + e.getMessage());
        }
    }

    public void stop() {
        logger.info("Deteniendo servidor HTTP");
        this.server.stop(0);
        this.server = null;
    }

}
