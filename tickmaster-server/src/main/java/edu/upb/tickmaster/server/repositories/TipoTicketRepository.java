package edu.upb.tickmaster.server.repositories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.upb.tickmaster.db.ConexionDb;

import java.sql.*;

/**
 * Repositorio para operaciones CRUD sobre la tabla 'tipo_ticket'.
 */
public class TipoTicketRepository {

    public TipoTicketRepository() {
    }

    /**
     * Crea un nuevo tipo de ticket para un evento.
     *
     * @return id generado del tipo de ticket
     */
    public int crear(int idEvento, String tipoAsiento, int cantidad, double precio) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, idEvento);
            pstmt.setString(2, tipoAsiento);
            pstmt.setInt(3, cantidad);
            pstmt.setDouble(4, precio);
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
     * Lista todos los tipos de ticket de un evento específico.
     */
    public JsonArray listarPorEvento(int idEvento) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT id_tipo_ticket, id_evento, tipo_asiento, cantidad, precio "
                + "FROM tipo_ticket WHERE id_evento = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEvento);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_tipo_ticket", rs.getInt("id_tipo_ticket"));
                    obj.addProperty("id_evento", rs.getInt("id_evento"));
                    obj.addProperty("tipo_asiento", rs.getString("tipo_asiento"));
                    obj.addProperty("cantidad", rs.getInt("cantidad"));
                    obj.addProperty("precio", rs.getDouble("precio"));
                    lista.add(obj);
                }
            }
        }
        return lista;
    }

    /**
     * Busca un tipo de ticket por su ID.
     *
     * @return JsonObject con los datos o null si no existe
     */
    public JsonObject findById(int idTipoTicket) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "SELECT id_tipo_ticket, id_evento, tipo_asiento, cantidad, precio "
                + "FROM tipo_ticket WHERE id_tipo_ticket = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idTipoTicket);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_tipo_ticket", rs.getInt("id_tipo_ticket"));
                    obj.addProperty("id_evento", rs.getInt("id_evento"));
                    obj.addProperty("tipo_asiento", rs.getString("tipo_asiento"));
                    obj.addProperty("cantidad", rs.getInt("cantidad"));
                    obj.addProperty("precio", rs.getDouble("precio"));
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Decrementa en 1 la disponibilidad de un tipo de ticket al comprarse.
     */
    public void decrementarDisponibilidad(int idTipoTicket) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "UPDATE tipo_ticket SET cantidad = cantidad - 1 WHERE id_tipo_ticket = ? AND cantidad > 0";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idTipoTicket);
            int filas = pstmt.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No hay disponibilidad para el tipo de ticket: " + idTipoTicket);
            }
        }
    }
}
