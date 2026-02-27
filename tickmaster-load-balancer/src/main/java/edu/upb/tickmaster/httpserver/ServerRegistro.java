package edu.upb.tickmaster.httpserver;

import java.util.ArrayList;
import java.util.List;

public class ServerRegistro {

    private static final List<String> servers = new ArrayList<>();

    public static synchronized void add(String ip, int puerto) {
        String server = "http://" + ip + ":" + puerto;
        if (!servers.contains(server)) {
            servers.add(server);
            System.out.println("Lista de servidores: " + servers);
        }
    }

    public static synchronized String getNextServer() {
        if (servers.isEmpty())
            return null;
        String server = servers.remove(0);
        servers.add(server);
        System.out.println("Rotacion de servidores: " + servers);
        return server;
    }

    public static synchronized List<String> getAll() {
        return new ArrayList<>(servers);
    }
}
