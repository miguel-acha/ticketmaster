package edu.upb.tickmaster.server.repositories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.upb.tickmaster.db.ConexionDb;

import java.sql.*;

/**
 * Repositorio para la tabla 'tickets'.
 * Adaptado al nuevo esquema: añade id_usuario, precio e id_tipo_ticket.
 */
public class TicketRepository {

    public TicketRepository() {
    }

    /**
     * Guarda un nuevo ticket en la base de datos.
     *
     * @param ticketId       UUID del ticket
     * @param idEvento       ID del evento
     * @param idUsuario      ID del usuario comprador
     * @param nroAsiento     Número o código de asiento
     * @param precio         Precio pagado
     * @param idempotencyKey Llave de idempotencia (puede ser null)
     * @param idTipoTicket   ID del tipo de ticket seleccionado
     */
    public void saveTicket(String ticketId, int idEvento, int idUsuario,
            String nroAsiento, double precio,
            String idempotencyKey, int idTipoTicket) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "INSERT INTO tickets "
                + "(id_ticket, id_evento, id_usuario, nro_asiento, precio, idempotency_key, id_tipo_ticket) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ticketId);
            pstmt.setInt(2, idEvento);
            pstmt.setInt(3, idUsuario);
            pstmt.setString(4, nroAsiento);
            pstmt.setDouble(5, precio);
            pstmt.setString(6, idempotencyKey);
            pstmt.setInt(7, idTipoTicket);
            pstmt.executeUpdate();
        }
    }

    /**
     * Busca un ticket por su llave de idempotencia.
     *
     * @return JsonObject con status/ticket_id/message, o null si no existe
     */
    public JsonObject findByIdempotencyKey(String key) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "SELECT id_ticket, id_evento, id_usuario, nro_asiento, precio "
                + "FROM tickets WHERE idempotency_key = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.addProperty("status", "OK");
                    jsonResponse.addProperty("ticket_id", rs.getString("id_ticket"));
                    jsonResponse.addProperty("id_evento", rs.getInt("id_evento"));
                    jsonResponse.addProperty("id_usuario", rs.getInt("id_usuario"));
                    jsonResponse.addProperty("nro_asiento", rs.getString("nro_asiento"));
                    jsonResponse.addProperty("precio", rs.getDouble("precio"));
                    jsonResponse.addProperty("message", "Ticket ya comprado anteriormente");
                    return jsonResponse;
                }
            }
        }
        return null;
    }

    /**
     * Lista todos los tickets de un usuario específico.
     */
    public JsonArray listarPorUsuario(int idUsuario) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT t.id_ticket, t.id_evento, t.nro_asiento, t.precio, "
                + "       e.nombre AS nombre_evento, e.fecha AS fecha_evento, "
                + "       tt.tipo_asiento "
                + "FROM tickets t "
                + "JOIN eventos e ON t.id_evento = e.id_evento "
                + "JOIN tipo_ticket tt ON t.id_tipo_ticket = tt.id_tipo_ticket "
                + "WHERE t.id_usuario = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_ticket", rs.getString("id_ticket"));
                    obj.addProperty("id_evento", rs.getInt("id_evento"));
                    obj.addProperty("nombre_evento", rs.getString("nombre_evento"));
                    obj.addProperty("fecha_evento", rs.getString("fecha_evento"));
                    obj.addProperty("tipo_asiento", rs.getString("tipo_asiento"));
                    obj.addProperty("nro_asiento", rs.getString("nro_asiento"));
                    obj.addProperty("precio", rs.getDouble("precio"));
                    lista.add(obj);
                }
            }
        }
        return lista;
    }

    /**
     * Lista todos los tickets (para uso administrativo).
     */
    public JsonArray listarTodos() throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT t.id_ticket, t.id_evento, t.id_usuario, t.nro_asiento, t.precio, "
                + "       e.nombre AS nombre_evento, u.username, tt.tipo_asiento "
                + "FROM tickets t "
                + "JOIN eventos e ON t.id_evento = e.id_evento "
                + "JOIN usuarios u ON t.id_usuario = u.id_usuario "
                + "JOIN tipo_ticket tt ON t.id_tipo_ticket = tt.id_tipo_ticket";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id_ticket", rs.getString("id_ticket"));
                obj.addProperty("id_evento", rs.getInt("id_evento"));
                obj.addProperty("nombre_evento", rs.getString("nombre_evento"));
                obj.addProperty("id_usuario", rs.getInt("id_usuario"));
                obj.addProperty("username", rs.getString("username"));
                obj.addProperty("tipo_asiento", rs.getString("tipo_asiento"));
                obj.addProperty("nro_asiento", rs.getString("nro_asiento"));
                obj.addProperty("precio", rs.getDouble("precio"));
                lista.add(obj);
            }
        }
        return lista;
    }
}
