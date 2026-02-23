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
 *
 * @author rlaredo
 * 
 */
public class ApacheServer {

    private static final Logger logger = LoggerFactory.getLogger(ApacheServer.class);
    private HttpServer server = null;
    private boolean isServerDone = false;

    public ApacheServer() {
    }

    public boolean start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(1914), 0);
            // --- REGISTRO DE RUTAS ---
            // El primer parámetro es el "contexto" (la URL que verá el cliente)
            // El segundo parámetro es el "handler" (la clase que procesa la petición)
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

            logger.info("Servidor HTTP iniciado en puerto 1914");
            return true;
        } catch (IOException e) {
            logger.error("Error al iniciar el servidor HTTP", e);
            this.server = null;
        }
        return false;
    }

    public void stop() {
        logger.info("Deteniendo servidor HTTP...");
        this.server.stop(0);
        this.server = null;
    }

}
