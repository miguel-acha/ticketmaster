package edu.upb.tickmaster.pasarela;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class PasarelaServer {
    private static final Logger logger = LoggerFactory.getLogger(PasarelaServer.class);
    private static final int PORT = 1916;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/cobrar", new PagoHandler());

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            logger.info("Pasarela de Pago iniciada en el puerto {}", PORT);
            logger.info("Esperando peticiones en /cobrar...");
        } catch (IOException e) {
            logger.error("Error al iniciar la pasarela de pago", e);
        }
    }
}
// mvn exec:java -pl tickmaster-pasarela
