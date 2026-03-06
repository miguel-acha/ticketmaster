package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.server.repositories.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Maneja las operaciones de usuarios: registro y login.
 *
 * POST /usuarios/registrar → registrar nuevo usuario
 * POST /usuarios/login → autenticar usuario
 * GET /usuarios → listar todos los usuarios (solo admin)
 */
public class UsuariosHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(UsuariosHandler.class);
    private final UsuarioRepository usuarioRepository;

    public UsuariosHandler() {
        this.usuarioRepository = new UsuarioRepository();
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            Headers responseHeaders = he.getResponseHeaders();
            responseHeaders.set("Content-type", ContentType.JSON.toString());

            String path = he.getRequestURI().getPath();
            String method = he.getRequestMethod();

            if (method.equals("OPTIONS")) {
                he.sendResponseHeaders(200, -1);
                return;
            }

            // POST /usuarios/registrar
            if (method.equals("POST") && path.endsWith("/registrar")) {
                handleRegistrar(he);
                return;
            }

            // POST /usuarios/login
            if (method.equals("POST") && path.endsWith("/login")) {
                handleLogin(he);
                return;
            }

            // GET /usuarios → listar todos
            if (method.equals("GET")) {
                handleListar(he);
                return;
            }

            sendJson(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");

        } catch (Exception e) {
            logger.error("Error inesperado en UsuariosHandler", e);
        }
    }

    private void handleRegistrar(HttpExchange he) throws IOException {
        String response;
        try {
            byte[] requestBody = he.getRequestBody().readAllBytes();
            String body = new String(requestBody, StandardCharsets.UTF_8);

            if (body.isEmpty()) {
                sendJson(he, 400, "{\"status\":\"NOK\",\"message\":\"Cuerpo de peticion vacio\"}");
                return;
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String username = json.get("username").getAsString();
            String nombre = json.get("nombre").getAsString();
            String password = json.get("password").getAsString();
            String rol = json.has("rol") ? json.get("rol").getAsString() : "cliente";

            int id = usuarioRepository.registrar(username, nombre, password, rol);

            logger.info("Usuario registrado: username={}, id={}", username, id);

            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("id_usuario", id);
            res.addProperty("message", "Usuario registrado exitosamente");
            response = res.toString();
            sendJson(he, 200, response);

        } catch (Exception e) {
            logger.error("Error al registrar usuario", e);
            response = "{\"status\":\"NOK\",\"message\":\"Error al registrar: " + e.getMessage() + "\"}";
            sendJson(he, 500, response);
        }
    }

    private void handleLogin(HttpExchange he) throws IOException {
        String response;
        try {
            byte[] requestBody = he.getRequestBody().readAllBytes();
            String body = new String(requestBody, StandardCharsets.UTF_8);

            if (body.isEmpty()) {
                sendJson(he, 400, "{\"status\":\"NOK\",\"message\":\"Cuerpo de peticion vacio\"}");
                return;
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String username = json.get("username").getAsString();
            String password = json.get("password").getAsString();

            JsonObject usuario = usuarioRepository.findByUsername(username);

            if (usuario == null) {
                logger.warn("Login fallido: usuario={} no encontrado", username);
                sendJson(he, 401, "{\"status\":\"NOK\",\"message\":\"Usuario no encontrado\"}");
                return;
            }

            try {
                if (!BCrypt.checkpw(password, usuario.get("password").getAsString())) {
                    logger.warn("Login fallido para username={}: contraseña incorrecta", username);
                    sendJson(he, 401, "{\"status\":\"NOK\",\"message\":\"Contraseña incorrecta\"}");
                    return;
                }
            } catch (IllegalArgumentException e) {
                logger.error("Error de hash para usuario={}: {}. Probablemente password en texto plano.", username,
                        e.getMessage());
                sendJson(he, 401,
                        "{\"status\":\"NOK\",\"message\":\"Error de seguridad: su cuenta requiere actualizar contraseña (hash incompatible)\"}");
                return;
            }

            logger.info("Login exitoso: username={}", username);

            JsonObject res = new JsonObject();
            res.addProperty("status", "OK");
            res.addProperty("id_usuario", usuario.get("id_usuario").getAsInt());
            res.addProperty("username", usuario.get("username").getAsString());
            res.addProperty("nombre", usuario.get("nombre").getAsString());
            res.addProperty("rol", usuario.get("rol").getAsString());
            response = res.toString();
            sendJson(he, 200, response);

        } catch (Exception e) {
            logger.error("Error en login", e);
            response = "{\"status\":\"NOK\",\"message\":\"Error en login: " + e.getMessage() + "\"}";
            sendJson(he, 500, response);
        }
    }

    private void handleListar(HttpExchange he) throws IOException {
        try {
            String response = usuarioRepository.listarTodos().toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            logger.error("Error al listar usuarios", e);
            sendJson(he, 500, "{\"status\":\"NOK\",\"message\":\"Error al listar usuarios\"}");
        }
    }

    private void sendJson(HttpExchange he, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }
}
