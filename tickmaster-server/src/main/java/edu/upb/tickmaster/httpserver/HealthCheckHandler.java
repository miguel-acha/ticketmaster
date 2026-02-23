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
        // --- 1. CONFIGURACIÓN DE RESPUESTA ---
        Headers responseHeaders = he.getResponseHeaders();
        // Definimos que la respuesta será un JSON
        responseHeaders.add("Content-Type", ContentType.JSON.toString());
        // Permitimos peticiones desde cualquier origen (CORS)
        responseHeaders.add("Access-Control-Allow-Origin", "*");

        // --- 2. CONSTRUCCIÓN DEL OBJETO JSON PRINCIPAL ---
        // Usamos la librería Gson para crear la estructura de datos
        JsonObject health = new JsonObject();
        health.addProperty("status", "OK");

        // --- 3. RECOPILACIÓN DE MÉTRICAS DE MEMORIA ---
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        // Creamos un sub-objeto para la memoria
        JsonObject memory = new JsonObject();
        memory.addProperty("used_mb", usedMemory / (1024 * 1024));
        memory.addProperty("free_mb", freeMemory / (1024 * 1024));
        memory.addProperty("total_mb", totalMemory / (1024 * 1024));
        memory.addProperty("max_mb", maxMemory / (1024 * 1024));

        // Agregamos el objeto de memoria al objeto principal 'health'
        health.add("memory", memory);

        // --- 4. VERIFICACIÓN DE ESPACIO EN DISCO ---
        File disk = new File(".");
        JsonObject diskInfo = new JsonObject();
        diskInfo.addProperty("total_gb", disk.getTotalSpace() / (1024 * 1024 * 1024));
        diskInfo.addProperty("free_gb", disk.getFreeSpace() / (1024 * 1024 * 1024));
        diskInfo.addProperty("usable_gb", disk.getUsableSpace() / (1024 * 1024 * 1024));

        // Agregamos la información del disco a la respuesta
        health.add("disk", diskInfo);

        // --- 5. VERIFICACIÓN DE ESTADO DE BASE DE DATOS ---
        JsonObject database = new JsonObject();
        try {
            // Intentamos obtener una conexión desde el Singleton de la DB
            Connection conn = ConexionDb.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                database.addProperty("status", "UP");
                database.addProperty("url", "jdbc:sqlite:tickmaster.db");
            } else {
                database.addProperty("status", "DOWN");
                database.addProperty("message", "Conexión cerrada");
                // Si la DB falla, el estado general se marca como DEGRADADO
                health.addProperty("status", "DEGRADED");
            }
        } catch (SQLException e) {
            logger.error("Error al verificar la base de datos", e);
            database.addProperty("status", "DOWN");
            database.addProperty("message", e.getMessage());
            health.addProperty("status", "DEGRADED");
        }
        health.add("database", database);

        // --- 6. ENVÍO DE LA RESPUESTA AL CLIENTE ---
        logger.debug("Health check ejecutado: {}", health.get("status"));

        // Convertimos el objeto JSON a una cadena de texto (String)
        String response = health.toString();
        // Lo convertimos a bytes usando codificación UTF-8
        byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);

        // Enviamos el código 200 (OK) y el tamaño del contenido
        he.sendResponseHeaders(200, byteResponse.length);

        // Escribimos los bytes en el cuerpo de la respuesta
        OutputStream os = he.getResponseBody();
        os.write(byteResponse);

        // Cerramos el stream para finalizar la comunicación
        os.close();
    }
}
