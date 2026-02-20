package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.db.ConexionDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

public class HealthCheckHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckHandler.class);

    @Override
    public void handle(HttpExchange he) throws IOException {
        Headers responseHeaders = he.getResponseHeaders();
        responseHeaders.add("Content-Type", ContentType.JSON.toString());
        responseHeaders.add("Access-Control-Allow-Origin", "*");

        JsonObject health = new JsonObject();
        health.addProperty("status", "OK");

        // Memoria de la aplicacion
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        JsonObject memory = new JsonObject();
        memory.addProperty("used_mb", usedMemory / (1024 * 1024));
        memory.addProperty("free_mb", freeMemory / (1024 * 1024));
        memory.addProperty("total_mb", totalMemory / (1024 * 1024));
        memory.addProperty("max_mb", maxMemory / (1024 * 1024));
        health.add("memory", memory);

        // Disco
        File disk = new File(".");
        JsonObject diskInfo = new JsonObject();
        diskInfo.addProperty("total_gb", disk.getTotalSpace() / (1024 * 1024 * 1024));
        diskInfo.addProperty("free_gb", disk.getFreeSpace() / (1024 * 1024 * 1024));
        diskInfo.addProperty("usable_gb", disk.getUsableSpace() / (1024 * 1024 * 1024));
        health.add("disk", diskInfo);

        // Base de datos
        JsonObject database = new JsonObject();
        try {
            Connection conn = ConexionDb.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                database.addProperty("status", "UP");
                database.addProperty("url", "jdbc:sqlite:tickmaster.db");
            } else {
                database.addProperty("status", "DOWN");
                database.addProperty("message", "Conexión cerrada");
                health.addProperty("status", "DEGRADED");
            }
        } catch (SQLException e) {
            logger.error("Error al verificar la base de datos", e);
            database.addProperty("status", "DOWN");
            database.addProperty("message", e.getMessage());
            health.addProperty("status", "DEGRADED");
        }
        health.add("database", database);

        logger.debug("Health check ejecutado: {}", health.get("status"));

        String response = health.toString();
        byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(200, byteResponse.length);
        OutputStream os = he.getResponseBody();
        os.write(byteResponse);
        os.close();
    }
}
