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
     * Crea un nuevo evento junto con sus tipos de tickets en una transacción.
     * La capacidad se calcula automáticamente como la suma de las cantidades de tipos de tickets.
     *
     * @return id generado del evento
     */
    public int crearEvento(String nombre, String fecha, String imagenUrl, JsonArray tiposTickets)
            throws SQLException {
        Connection conn = null;
        try {
            conn = ConexionDb.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Calcular capacidad como suma de cantidades de tickets
            int capacidadTotal = 0;
            if (tiposTickets != null) {
                for (int i = 0; i < tiposTickets.size(); i++) {
                    com.google.gson.JsonElement el = tiposTickets.get(i);
                    if (el != null && el.isJsonObject()) {
                        JsonObject tobj = el.getAsJsonObject();
                        if (tobj.has("cantidad") && !tobj.get("cantidad").isJsonNull()) {
                            capacidadTotal += tobj.get("cantidad").getAsInt();
                        }
                    }
                }
            }

            int idEvento = -1;
            String sqlEvento = "INSERT INTO eventos (nombre, fecha, capacidad, imagen_url) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlEvento, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, nombre);
                pstmt.setString(2, fecha);
                pstmt.setInt(3, capacidadTotal);
                pstmt.setString(4, imagenUrl);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        idEvento = rs.getInt(1);
                    }
                }
            }

            if (idEvento == -1) {
                conn.rollback();
                return -1;
            }

            if (tiposTickets != null && tiposTickets.size() > 0) {
                String sqlTipo = "INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmtTipo = conn.prepareStatement(sqlTipo)) {
                    for (int i = 0; i < tiposTickets.size(); i++) {
                        com.google.gson.JsonElement el = tiposTickets.get(i);
                        if (el == null || !el.isJsonObject()) continue;
                        
                        JsonObject tipo = el.getAsJsonObject();
                        String asiento = (tipo.has("tipo_asiento") && !tipo.get("tipo_asiento").isJsonNull()) ? tipo.get("tipo_asiento").getAsString() : "General";
                        int cant = (tipo.has("cantidad") && !tipo.get("cantidad").isJsonNull()) ? tipo.get("cantidad").getAsInt() : 0;
                        double prc = (tipo.has("precio") && !tipo.get("precio").isJsonNull()) ? tipo.get("precio").getAsDouble() : 0.0;

                        pstmtTipo.setInt(1, idEvento);
                        pstmtTipo.setString(2, asiento);
                        pstmtTipo.setInt(3, cant);
                        pstmtTipo.setDouble(4, prc);
                        pstmtTipo.addBatch();
                    }
                    pstmtTipo.executeBatch();
                }
            }

            conn.commit(); // Confirmar transacción
            return idEvento;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true); // Restaurar autocommit
            }
        }
    }

    /**
     * Lista todos los eventos disponibles.
     */
    public JsonArray listarEventos() throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT e.id_evento, e.nombre, e.fecha, e.imagen_url, "
                + "(SELECT COALESCE(SUM(tt.cantidad), 0) FROM tipo_ticket tt WHERE tt.id_evento = e.id_evento) as capacidad "
                + "FROM eventos e ORDER BY e.fecha ASC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id_evento", rs.getInt("id_evento"));
                obj.addProperty("nombre", rs.getString("nombre"));
                obj.addProperty("fecha", rs.getString("fecha"));
                obj.addProperty("capacidad", rs.getInt("capacidad"));
                obj.addProperty("imagen_url", rs.getString("imagen_url"));
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
        String sql = "SELECT e.id_evento, e.nombre, e.fecha, e.imagen_url, "
                + "(SELECT COALESCE(SUM(tt.cantidad), 0) FROM tipo_ticket tt WHERE tt.id_evento = e.id_evento) as capacidad "
                + "FROM eventos e WHERE e.id_evento = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEvento);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_evento", rs.getInt("id_evento"));
                    obj.addProperty("nombre", rs.getString("nombre"));
                    obj.addProperty("fecha", rs.getString("fecha"));
                    obj.addProperty("capacidad", rs.getInt("capacidad"));
                    obj.addProperty("imagen_url", rs.getString("imagen_url"));
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Aumenta la capacidad del evento cuando se agregan nuevos tickets.
     */
    public void aumentarCapacidad(int idEvento, int cantidad) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "UPDATE eventos SET capacidad = capacidad + ? WHERE id_evento = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cantidad);
            pstmt.setInt(2, idEvento);
            pstmt.executeUpdate();
        }
    }

    /**
     * Elimina un evento y todas sus dependencias.
     */
    public void eliminarEvento(int idEvento) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tickets WHERE id_evento = ?")) {
            pstmt.setInt(1, idEvento);
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tipo_ticket WHERE id_evento = ?")) {
            pstmt.setInt(1, idEvento);
            pstmt.executeUpdate();
        }

        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM eventos WHERE id_evento = ?")) {
            pstmt.setInt(1, idEvento);
            pstmt.executeUpdate();
        }
    }
}
