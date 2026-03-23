package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import edu.upb.tickmaster.server.repositories.CompraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handler para gestionar peticiones relacionadas con el historial de compras.
 */
public class ComprasHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ComprasHandler.class);
    private CompraRepository compraRepository;

    public ComprasHandler() {
        this.compraRepository = new CompraRepository();
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.add("Content-type", ContentType.JSON.toString());

            String method = he.getRequestMethod();

            if (method.equals("OPTIONS")) {
                he.sendResponseHeaders(200, -1);
                return;
            }

            if (method.equals("GET")) {
                String query = he.getRequestURI().getQuery();
                if (query != null && query.contains("usuario=")) {
                    int idUsuario = Integer.parseInt(query.split("usuario=")[1].split("&")[0]);
                    String json = compraRepository.listarPorUsuario(idUsuario).toString();
                    enviarRespuestaJson(he, 200, json);
                } else {
                    enviarRespuestaJson(he, 400, "{\"status\":\"NOK\",\"message\":\"Falta parametro de busqueda usuario\"}");
                }
            } else {
                enviarRespuestaJson(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no permitido\"}");
            }

        } catch (Exception e) {
            logger.error("Error en ComprasHandler", e);
            enviarRespuestaJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error interno en el servidor\"}");
        }
    }

    private void enviarRespuestaJson(HttpExchange he, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }
}
