package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro de servidores backend con soporte para múltiples servicios,
 * Round-Robin y Health Check integrado.
 */
public class ServerRegistro extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ServerRegistro.class);
    private static ServerRegistro instance;

    // Lista de balanceo: Servidores que están respondiendo OK
    private final Map<String, List<String>> listaBalanceo = new ConcurrentHashMap<>();
    // Lista de verificación: Todos los servidores registrados y en proceso de
    // monitoreo
    private final Map<String, List<String>> listaVerificacion = new ConcurrentHashMap<>();
    // Contador de fallos por servidor
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> failureCounts = new ConcurrentHashMap<>();

    // Configuración del límite de servidores
    private static final int MAX_SERVERS = 3;
    private static final boolean LIMIT_ENABLED = false; // Cambiar a false para desactivar el límite
    public static final String intentos = System.getenv("INTENTOS") != null ? System.getenv("INTENTOS") : "3";

    private ServerRegistro() {
        // Configuramos el hilo como daemon ya que es una funcion de background
        this.setDaemon(true);
        this.setName("HiloSaludBalanceador");
        this.start();
        logger.info("Hilo de health check iniciado automáticamente.");
    }

    public static synchronized ServerRegistro getInstance() {
        if (instance == null) {
            instance = new ServerRegistro();
        }
        return instance;
    }

    @Override
    public void run() {
        try {
            // Esperar un poco antes del primer chequeo para que los servidores arranquen
            Thread.sleep(10000);
            while (true) {
                verificarSaludTodos();
                Thread.sleep(2000); // Antes 6000 (6s), ahora 2s para recuperación rápida
            }
        } catch (InterruptedException e) {
            logger.error("Hilo de salud interrumpido", e);
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void add(String serviceName, String ip, int puerto) {
        String server = "http://" + ip + ":" + puerto;
        add(serviceName, server);
    }

    public synchronized void add(String serviceName, String serverUrl) {
        String cleanUrl = cleanUrl(serverUrl);
        listaVerificacion.computeIfAbsent(serviceName, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        listaBalanceo.computeIfAbsent(serviceName, k -> new java.util.concurrent.CopyOnWriteArrayList<>());

        if (!listaVerificacion.get(serviceName).contains(cleanUrl)) {
            // Verificar límite si está activado
            if (LIMIT_ENABLED && listaVerificacion.get(serviceName).size() >= MAX_SERVERS) {
                logger.warn("No se pudo registrar {}: Límite máximo de {} servidores alcanzado para el servicio {}.",
                        cleanUrl, MAX_SERVERS, serviceName);
                return;
            }
            listaVerificacion.get(serviceName).add(cleanUrl);
            logger.info("Servidor registrado en [{}]: {}", serviceName, cleanUrl);
        }

        // lo consideramos activo inicialmente para no esperar al prime
        if (!listaBalanceo.get(serviceName).contains(cleanUrl)) {
            listaBalanceo.get(serviceName).add(cleanUrl);
        }
    }

    private String cleanUrl(String url) {
        if (url == null)
            return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public synchronized String getNextServer(String serviceName) {
        List<String> servers = listaBalanceo.get(serviceName);
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        // Round-Robin
        String server = servers.remove(0);
        servers.add(server);

        return server;
    }

    private void verificarSaludTodos() {
        logger.debug("Verificando health de todos...");

        listaVerificacion.forEach((serviceName, servers) -> {
            for (String url : servers) {
                boolean estaSano = realizarChequeo(url);
                actualizarEstadoServidor(serviceName, url, estaSano);
            }
        });
    }

    private boolean realizarChequeo(String serverUrl) {
        try {
            URL url = new URL(serverUrl + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                return "UP".equalsIgnoreCase(json.get("status").getAsString());
            }
        } catch (Exception e) {
            logger.debug("Servidor {} no saludable: {}", serverUrl, e.getMessage());
        }
        return false;
    }

    private synchronized void actualizarEstadoServidor(String serviceName, String url, boolean estaSano) {
        List<String> balanceo = listaBalanceo.get(serviceName);
        if (estaSano) {
            failureCounts.computeIfAbsent(url, k -> new java.util.concurrent.atomic.AtomicInteger(0)).set(0);
            if (balanceo != null && !balanceo.contains(url)) {
                balanceo.add(url);
                logger.info("Servidor recuperado a lista de balanceo [{}]: {}", serviceName, url);
            }
        } else {
            // Si el chequeo falla, se reporta como un fallo
            reportFailure(serviceName, url);
        }
    }

    public synchronized void reportFailure(String serviceName, String url) {
        List<String> balanceo = listaBalanceo.get(serviceName);
        List<String> verificacion = listaVerificacion.get(serviceName);

        int max = Integer.parseInt(intentos);
        int currentFallos = failureCounts.computeIfAbsent(url, k -> new java.util.concurrent.atomic.AtomicInteger(0))
                .incrementAndGet();

        // Al primer fallo se saca de la lista de balanceo inmediatamente
        if (balanceo != null && balanceo.remove(url)) {
            logger.warn("Servidor sacado de balanceo al primer fallo [{}]: {}", serviceName, url);
        }

        // Si llega a los 3 intentos(max), se saca de la lista de verificación
        // (definitivamente)
        if (currentFallos >= max) {
            if (verificacion != null && verificacion.remove(url)) {
                logger.error("Servidor sacado definitivamente de verificación tras {} fallos [{}]: {}", max,
                        serviceName, url);
            }
        } else {
            logger.warn("Fallo detectado en verificación ({}/{}) para [{}]: {}", currentFallos, max, serviceName, url);
        }
    }

    public Map<String, List<String>> getAll() {
        return new ConcurrentHashMap<>(listaBalanceo);
    }
}
