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
            try {
                logger.info("Presiona Ctrl+C para detener...");
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                logger.info("Interrumpido, deteniendo...");
            }
        } else {
            logger.error("No se pudo iniciar el servidor");
        }
    }
}

// mvn exec:java -pl tickmaster-server
