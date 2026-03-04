package edu.upb.tickmaster.server.repositories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.upb.tickmaster.db.ConexionDb;

import java.sql.*;

/**
 * Repositorio para operaciones CRUD sobre la tabla 'eventos'.
 */
public class EventoRepository {

    public EventoRepository() {
    }

    /**
     * Crea un nuevo evento.
     *
     * @return id generado del evento
     */
    public int crearEvento(String nombre, String fecha, int capacidad) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "INSERT INTO eventos (nombre, fecha, capacidad) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, fecha);
            pstmt.setInt(3, capacidad);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Lista todos los eventos disponibles.
     */
    public JsonArray listarEventos() throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT id_evento, nombre, fecha, capacidad FROM eventos ORDER BY fecha ASC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id_evento", rs.getInt("id_evento"));
                obj.addProperty("nombre", rs.getString("nombre"));
                obj.addProperty("fecha", rs.getString("fecha"));
                obj.addProperty("capacidad", rs.getInt("capacidad"));
                lista.add(obj);
            }
        }
        return lista;
    }

    /**
     * Busca un evento por su ID.
     *
     * @return JsonObject con los datos del evento o null si no existe
     */
    public JsonObject findById(int idEvento) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "SELECT id_evento, nombre, fecha, capacidad FROM eventos WHERE id_evento = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEvento);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_evento", rs.getInt("id_evento"));
                    obj.addProperty("nombre", rs.getString("nombre"));
                    obj.addProperty("fecha", rs.getString("fecha"));
                    obj.addProperty("capacidad", rs.getInt("capacidad"));
                    return obj;
                }
            }
        }
        return null;
    }
}
