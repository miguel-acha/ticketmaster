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
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Load Balancer Server que actúa como proxy
 * 
 * @author rlaredo
 */
public class LoadBalancerServer {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerServer.class);
    private HttpServer server = null;
    private final String backendServerUrl = "http://localhost:1914";
    // private final String backendServerUrl = "http://10.255.255.1:1914";
    // GET http://localhost:1915/tickets

    public LoadBalancerServer() {
    }

    public boolean start() {
        try {
            // Crear servidor en puerto 1915
            this.server = HttpServer.create(new InetSocketAddress(1915), 0);

            logger.info("Iniciando en puerto 1915");
            logger.info("Backend server: {}", backendServerUrl);

            // Crear proxy handler para todas las rutas
            ProxyHandler proxyHandler = new ProxyHandler(backendServerUrl);

            // Configurar CORS para todas las rutas
            this.server.createContext("/", exchange -> {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
                headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

                // Health check
                if (exchange.getRequestURI().getPath().equals("/health")) {
                    String response = "{\"status\": \"OK\"}";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                }

                // Si es OPTIONS, responder directamente
                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                } else {
                    // Reenviar al proxy handler
                    proxyHandler.handle(exchange);
                }
            });

            this.server.setExecutor(Executors.newFixedThreadPool(10));
            this.server.start();

            logger.info("Servidor iniciado correctamente");

            return true;
        } catch (IOException e) {
            logger.error("Error al iniciar el servidor", e);
            this.server = null;
        }
        return false;
    }

    public void stop() {
        if (this.server != null) {
            logger.info("Deteniendo servidor...");
            this.server.stop(0);
            this.server = null;
        }
    }
}
