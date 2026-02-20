package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
// import edu.upb.tickmaster.db.ConexionDb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

/**
 * @author rlaredo
 */
/*
 * public class UsuariosHandler implements HttpHandler {
 * 
 * public UsuariosHandler() {
 * 
 * }
 * 
 * @Override
 * public void handle(HttpExchange he) throws IOException {
 * 
 * try {
 * InputStreamReader isr = new InputStreamReader(he.getRequestBody(),
 * StandardCharsets.UTF_8);
 * String response = "";
 * BufferedReader br = new BufferedReader(isr);
 * Headers responseHeaders = he.getResponseHeaders();
 * responseHeaders.add("Access-Control-Allow-Origin", "*");
 * responseHeaders.add("Content-type", ContentType.JSON.toString());
 * 
 * if (he.getRequestMethod().equals("POST")) {
 * try (InputStream is = he.getRequestBody()) {
 * 
 * // Read request body
 * Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name());
 * String requestBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() :
 * "";
 * 
 * // Parse JSON
 * JsonObject jsonRequest = new
 * com.google.gson.JsonParser().parse(requestBody).getAsJsonObject();
 * // String codigo = jsonRequest.get("codigo").getAsString();
 * String id = jsonRequest.get("id").getAsString();
 * String nombre = jsonRequest.get("nombre").getAsString();
 * String ip = jsonRequest.get("ip").getAsString();
 * 
 * // Insert into database
 * Connection conn = ConexionDb.getInstance().getConnection();
 * try (PreparedStatement pstmt = conn
 * .prepareStatement("INSERT INTO jugadores (id, nombre, ip) VALUES (?, ?, ?)"))
 * {
 * pstmt.setString(1, id);
 * pstmt.setString(2, nombre);
 * pstmt.setString(3, ip);
 * pstmt.executeUpdate();
 * }
 * 
 * // Send response
 * JsonObject jsonResponse = new JsonObject();
 * jsonResponse.addProperty("status", "OK");
 * response = jsonResponse.toString();
 * 
 * he.sendResponseHeaders(Integer.parseInt(Status._200.name().substring(1, 4)),
 * response.length());
 * } catch (Exception e) {
 * response =
 * "{\"status\": \"NOK\",\"message\": \"No se logro imprir la factura\"}";
 * he.sendResponseHeaders(Integer.parseInt(Status._200.name().substring(1, 4)),
 * response.length());
 * 
 * }
 * OutputStream os = he.getResponseBody();
 * os.write(response.getBytes());
 * os.close();
 * return;
 * 
 * }
 * 
 * if (he.getRequestMethod().equals("GET")) {
 * 
 * try {
 * Connection conn = ConexionDb.getInstance().getConnection();
 * Statement stmt = conn.createStatement();
 * ResultSet rs = stmt.executeQuery("SELECT * FROM jugadores");
 * 
 * JsonArray players = new JsonArray();
 * while (rs.next()) {
 * JsonObject player = new JsonObject();
 * player.addProperty("id", rs.getInt("id"));
 * player.addProperty("nombre", rs.getString("nombre"));
 * player.addProperty("ip", rs.getString("ip"));
 * players.add(player);
 * }
 * response = players.toString();
 * byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
 * he.sendResponseHeaders(Integer.parseInt(Status._200.name().substring(1, 4)),
 * byteResponse.length);
 * OutputStream os = he.getResponseBody();
 * os.write(byteResponse);
 * os.close();
 * 
 * } catch (Exception e) {
 * e.printStackTrace();
 * response =
 * "{\"status\": \"NOK\",\"message\": \"Error al leer de base de datos: " +
 * e.getMessage()
 * + "\"}";
 * he.sendResponseHeaders(Integer.parseInt(Status._500.name().substring(1, 4)),
 * response.length());
 * OutputStream os = he.getResponseBody();
 * os.write(response.getBytes());
 * os.close();
 * }
 * return;
 * }
 * 
 * if (he.getRequestMethod().equals("OPTIONS")) {
 * response =
 * "{\"status\": \"OK\",\"message\": \"Factura impreso correctamente\"}";
 * he.sendResponseHeaders(Integer.parseInt(Status._200.name().substring(1, 4)),
 * response.length());
 * } else {
 * response = "{\"status\": \"NOK\",\"message\": \"Methodo no soportado\"}";
 * he.sendResponseHeaders(Integer.parseInt(Status._404.name().substring(1, 4)),
 * response.length());
 * }
 * OutputStream os = he.getResponseBody();
 * os.write(response.getBytes());
 * os.close();
 * } catch (NumberFormatException | IOException e) {
 * e.printStackTrace();
 * }
 * }
 * }
 */