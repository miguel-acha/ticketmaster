package edu.upb.tickmaster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.upb.tickmaster.db.ConexionDb;
import edu.upb.tickmaster.httpserver.ApacheServer;
import java.io.*;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        String archivoConfig = "config.acha";
        Configuracion miConfig = new Configuracion("localhost", 8080, "miguel");

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivoConfig))) {
            out.writeObject(miConfig);
            logger.info("Configuracion guardada en {}", archivoConfig);
        } catch (IOException e) {
            logger.error("Error al guardar configuración", e);
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivoConfig))) {
            Configuracion configCargada = (Configuracion) in.readObject();
            logger.info("Configuracion cargada: {}", configCargada);
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error al cargar configuracion", e);
        }

        // ------------------------------------------------

        logger.info("Iniciando Tickmaster Server...");
        ConexionDb.getInstance();
        ApacheServer apacheServer = new ApacheServer();
        if (apacheServer.start()) {
            logger.info("Tickmaster Server iniciado correctamente en puerto 1914");
        } else {
            logger.error("No se pudo iniciar el servidor");
        }
    }
}

// mvn exec:java -pl tickmaster-server

/*
 * Guia para agregar una nueva funcon de API
 * Para agregar una nueva funcionalidad de API en este proyecto, sigue estos
 * tres pasos principales:
 * 
 * 1. Crear el Handler
 * Crea una nueva clase en el paquete edu.upb.tickmaster.httpserver que
 * implemente la interfaz HttpHandler.
 * 
 * Ejemplo (MiNuevaFuncionHandler.java):
 * 
 * java
 * package edu.upb.tickmaster.httpserver;
 * import com.sun.net.httpserver.HttpExchange;
 * import com.sun.net.httpserver.HttpHandler;
 * import java.io.IOException;
 * import java.io.OutputStream;
 * import java.nio.charset.StandardCharsets;
 * public class MiNuevaFuncionHandler implements HttpHandler {
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * // 1. Configurar headers (opcional)
 * exchange.getResponseHeaders().add("Content-Type", "application/json");
 * 
 * // 2. Definir la respuesta
 * String response = "{\"mensaje\": \"Hola desde la nueva API\"}";
 * byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
 * 
 * // 3. Enviar headers de respuesta (Status 200 OK)
 * exchange.sendResponseHeaders(200, bytes.length);
 * 
 * // 4. Escribir el cuerpo y cerrar
 * try (OutputStream os = exchange.getResponseBody()) {
 * os.write(bytes);
 * }
 * }
 * }
 * 2. Registrar el Contexto
 * Abre el archivo
 * ApacheServer.java
 * y agrega la nueva ruta en el método
 * start()
 * .
 * 
 * Cambio en
 * ApacheServer.java
 * :
 * 
 * java
 * this.server.createContext("/mi-nueva-ruta", new MiNuevaFuncionHandler());
 * 3. Reiniciar el Servidor
 * Compila y ejecuta el proyecto nuevamente para que los cambios surtan efecto.
 * 
 * Tips adicionales:
 * Parsear query params: Puedes usar el método estático
 * RootHandler.parseQuery(exchange.getRequestURI().getQuery(), parameters) para
 * obtener los parámetros de la URL.
 * CORS: El servidor ya tiene configurado CORS básico en el contexto raíz /,
 * pero asegúrate de agregarlo en tu handler si el contexto es diferente y lo
 * necesitas.
 * 
 * 
 */