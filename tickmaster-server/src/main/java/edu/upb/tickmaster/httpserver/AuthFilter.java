package edu.upb.tickmaster.httpserver;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Filtro simple para validar el token JWT en las peticiones.
 */
public class AuthFilter extends Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        // Ignorar OPTIONS para CORS
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            chain.doFilter(exchange);
            return;
        }

        // Obtener cabecera Authorization: Bearer <token>
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(exchange, 401, "Se requiere token de autenticacion");
            return;
        }

        String token = authHeader.substring(7);
        DecodedJWT decoded = JwtUtil.verifyToken(token);

        if (decoded == null) {
            sendError(exchange, 401, "Token invalido o expirado");
            return;
        }

        // Guardar datos del usuario en atributos para que los handlers los usen
        exchange.setAttribute("id_usuario", decoded.getClaim("id_usuario").asInt());
        exchange.setAttribute("rol", decoded.getClaim("rol").asString());
        exchange.setAttribute("username", decoded.getSubject());

        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "Filtro de autenticacion JWT";
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = "{\"status\":\"NOK\",\"message\":\"" + message + "\"}";
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
