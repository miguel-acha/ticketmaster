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

    // Servidores que están respondiendo OK
    private final Map<String, List<String>> activeServers = new ConcurrentHashMap<>();
    // Todos los servidores registrados (vía config o dinámico)
    private final Map<String, List<String>> allRegisteredServers = new ConcurrentHashMap<>();

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
                Thread.sleep(60000); // 1 minuto
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
        allRegisteredServers.computeIfAbsent(serviceName, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        activeServers.computeIfAbsent(serviceName, k -> new java.util.concurrent.CopyOnWriteArrayList<>());

        if (!allRegisteredServers.get(serviceName).contains(serverUrl)) {
            allRegisteredServers.get(serviceName).add(serverUrl);
            logger.info("Servidor registrado en [{}]: {}", serviceName, serverUrl);
        }

        // lo consideramos activo inicialmente para no esperar al prime
        if (!activeServers.get(serviceName).contains(serverUrl)) {
            activeServers.get(serviceName).add(serverUrl);
        }
    }

    public synchronized String getNextServer(String serviceName) {
        List<String> servers = activeServers.get(serviceName);
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
        allRegisteredServers.forEach((serviceName, servers) -> {
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
                return "OK".equalsIgnoreCase(json.get("status").getAsString());
            }
        } catch (Exception e) {
            logger.debug("Servidor {} no saludable: {}", serverUrl, e.getMessage());
        }
        return false;
    }

    private synchronized void actualizarEstadoServidor(String serviceName, String url, boolean estaSano) {
        List<String> active = activeServers.get(serviceName);
        if (estaSano) {
            if (!active.contains(url)) {
                active.add(url);
                logger.info("Servidor recuperado [{}]: {}", serviceName, url);
            }
        } else {
            if (active.remove(url)) {
                logger.warn("Servidor fuera de servicio [{}]: {}", serviceName, url);
            }
        }
    }

    public Map<String, List<String>> getAll() {
        return new ConcurrentHashMap<>(activeServers);
    }
}
