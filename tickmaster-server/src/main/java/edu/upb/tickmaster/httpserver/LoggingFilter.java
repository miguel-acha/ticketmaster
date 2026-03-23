package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filtro para loguear todas las peticiones entrantes al servidor.
 */
public class LoggingFilter extends Filter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().toString();
        String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();

        logger.info("PETICION RECIBIDA: [{}] {} desde {}", method, path, clientIP);

        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "Filtro de logging para peticiones entrantes";
    }
}
