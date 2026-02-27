package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tickmaster.db.ConexionDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

public class RegistrarHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(RegistrarHandler.class);

    @Override
    public void handle(HttpExchange he) throws IOException {

    }
}
