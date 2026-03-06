package edu.upb.tickmaster.httpserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro de servidores backend con soporte para múltiples servicios y Round-Robin.
 */
public class ServerRegistro {
    private static final Logger logger = LoggerFactory.getLogger(ServerRegistro.class);
    private static final Map<String, List<String>> serviceRegistry = new ConcurrentHashMap<>();

    public static synchronized void add(String serviceName, String ip, int puerto) {
        String server = "http://" + ip + ":" + puerto;
        add(serviceName, server);
    }

    public static synchronized void add(String serviceName, String serverUrl) {
        serviceRegistry.computeIfAbsent(serviceName, k -> new ArrayList<>());
        
        List<String> servers = serviceRegistry.get(serviceName);
        if (!servers.contains(serverUrl)) {
            servers.add(serverUrl);
            logger.info("Servidor registrado para [{}]: {}", serviceName, serverUrl);
        }
    }

    public static synchronized String getNextServer(String serviceName) {
        List<String> servers = serviceRegistry.get(serviceName);
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        
        // Round-Robin: Quitamos el primero y lo ponemos al final
        String server = servers.remove(0);
        servers.add(server);
        
        return server;
    }

    public static Map<String, List<String>> getAll() {
        return new ConcurrentHashMap<>(serviceRegistry);
    }
}
