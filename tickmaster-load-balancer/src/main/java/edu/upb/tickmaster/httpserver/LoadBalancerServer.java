package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Servidor del Balanceador de Carga.
 * Actúa como un API Gateway dinámico que redirige peticiones a clusters de
 * microservicios.
 */
public class LoadBalancerServer {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerServer.class);
    private HttpServer server = null;
    private final int PUERTO = 1915;

    public boolean start() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(PUERTO), 0);

            logger.info("Iniciando Balanceador de Carga en puerto {}", PUERTO);

            // ProxyHandler maneja todo (Routing, CORS, Registro, Round-Robin)
            this.server.createContext("/", new ProxyHandler());

            this.server.setExecutor(Executors.newFixedThreadPool(20));
            this.server.start();

            logger.info("Balanceador listo y escuchando peticiones.");
            return true;
        } catch (IOException e) {
            logger.error("Error al iniciar el balanceador", e);
            return false;
        }
    }

    public void stop() {
        if (server != null) {
            logger.info("Deteniendo balanceador...");
            server.stop(0);
        }
    }
}
