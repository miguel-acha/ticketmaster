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

    public LoadBalancerServer() {
    }

    public boolean start() {
        try {
            // Crear servidor en puerto 1915
            this.server = HttpServer.create(new InetSocketAddress(1915), 0);

            logger.info("Iniciando Balanceador en puerto 1915");

            // Configurar CORS para todas las rutas
            this.server.createContext("/", exchange -> {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
                headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

                // Health check propio del balanceador
                if (exchange.getRequestURI().getPath().equals("/lb-health")) {
                    String response = "{\"status\": \"OK\", \"servidores\": " + ServerRegistro.getAll().toString()
                            + "}";
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
                    return;
                }

                try {
                    new ProxyHandler().handle(exchange);
                } catch (Exception e) {
                    logger.error("Error en el proxy: " + e.getMessage());
                }
            });

            this.server.createContext("/registrar", new RegistroHandler());

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
