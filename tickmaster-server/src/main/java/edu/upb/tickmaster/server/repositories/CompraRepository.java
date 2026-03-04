package edu.upb.tickmaster.server.repositories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.upb.tickmaster.db.ConexionDb;

import java.sql.*;

/**
 * Repositorio para operaciones CRUD sobre la tabla 'compras'.
 */
public class CompraRepository {

    public CompraRepository() {
    }

    /**
     * Registra una nueva compra en la base de datos.
     *
     * @param idTicket  UUID del ticket comprado
     * @param idUsuario ID del usuario que compra
     * @param total     Precio total pagado
     * @return id generado de la compra
     */
    public int registrarCompra(String idTicket, int idUsuario, double total) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "INSERT INTO compras (id_ticket, id_usuario, total, estado) VALUES (?, ?, ?, 'completada')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, idTicket);
            pstmt.setInt(2, idUsuario);
            pstmt.setDouble(3, total);
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
     * Lista todas las compras de un usuario específico (incluyendo datos del ticket
     * y evento).
     */
    public JsonArray listarPorUsuario(int idUsuario) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT c.id_compra, c.id_ticket, c.fecha_compra, c.total, c.estado, "
                + "       e.nombre AS nombre_evento, e.fecha AS fecha_evento, "
                + "       t.nro_asiento, tt.tipo_asiento "
                + "FROM compras c "
                + "JOIN tickets t ON c.id_ticket = t.id_ticket "
                + "JOIN eventos e ON t.id_evento = e.id_evento "
                + "JOIN tipo_ticket tt ON t.id_tipo_ticket = tt.id_tipo_ticket "
                + "WHERE c.id_usuario = ? "
                + "ORDER BY c.fecha_compra DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_compra", rs.getInt("id_compra"));
                    obj.addProperty("id_ticket", rs.getString("id_ticket"));
                    obj.addProperty("fecha_compra", rs.getString("fecha_compra"));
                    obj.addProperty("total", rs.getDouble("total"));
                    obj.addProperty("estado", rs.getString("estado"));
                    obj.addProperty("nombre_evento", rs.getString("nombre_evento"));
                    obj.addProperty("fecha_evento", rs.getString("fecha_evento"));
                    obj.addProperty("nro_asiento", rs.getString("nro_asiento"));
                    obj.addProperty("tipo_asiento", rs.getString("tipo_asiento"));
                    lista.add(obj);
                }
            }
        }
        return lista;
    }

    /**
     * Lista todas las compras (para uso del admin).
     */
    public JsonArray listarTodas() throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT c.id_compra, c.id_ticket, c.id_usuario, c.fecha_compra, c.total, c.estado, "
                + "       u.username, e.nombre AS nombre_evento "
                + "FROM compras c "
                + "JOIN usuarios u ON c.id_usuario = u.id_usuario "
                + "JOIN tickets t ON c.id_ticket = t.id_ticket "
                + "JOIN eventos e ON t.id_evento = e.id_evento "
                + "ORDER BY c.fecha_compra DESC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id_compra", rs.getInt("id_compra"));
                obj.addProperty("id_ticket", rs.getString("id_ticket"));
                obj.addProperty("id_usuario", rs.getInt("id_usuario"));
                obj.addProperty("username", rs.getString("username"));
                obj.addProperty("nombre_evento", rs.getString("nombre_evento"));
                obj.addProperty("fecha_compra", rs.getString("fecha_compra"));
                obj.addProperty("total", rs.getDouble("total"));
                obj.addProperty("estado", rs.getString("estado"));
                lista.add(obj);
            }
        }
        return lista;
    }
}
