package edu.upb.tickmaster.server.repositories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.upb.tickmaster.db.ConexionDb;

import java.sql.*;

/**
 * Repositorio para operaciones CRUD sobre la tabla 'usuarios'.
 */
public class UsuarioRepository {

    public UsuarioRepository() {
    }

    /**
     * Registra un nuevo usuario en la base de datos.
     *
     * @return id generado del usuario
     */
    public int registrar(String username, String nombre, String password, String rol) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "INSERT INTO usuarios (username, nombre, password, rol) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, nombre);
            pstmt.setString(3, password);
            pstmt.setString(4, rol != null ? rol : "cliente");
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
     * Busca un usuario por su username (para login).
     *
     * @return JsonObject con los datos del usuario o null si no existe
     */
    public JsonObject findByUsername(String username) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "SELECT id_usuario, username, nombre, password, rol FROM usuarios WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_usuario", rs.getInt("id_usuario"));
                    obj.addProperty("username", rs.getString("username"));
                    obj.addProperty("nombre", rs.getString("nombre"));
                    obj.addProperty("password", rs.getString("password"));
                    obj.addProperty("rol", rs.getString("rol"));
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Busca un usuario por su ID.
     *
     * @return JsonObject con los datos del usuario o null si no existe
     */
    public JsonObject findById(int idUsuario) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "SELECT id_usuario, username, nombre, rol FROM usuarios WHERE id_usuario = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id_usuario", rs.getInt("id_usuario"));
                    obj.addProperty("username", rs.getString("username"));
                    obj.addProperty("nombre", rs.getString("nombre"));
                    obj.addProperty("rol", rs.getString("rol"));
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * Lista todos los usuarios (sin exponer passwords).
     */
    public JsonArray listarTodos() throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray lista = new JsonArray();
        String sql = "SELECT id_usuario, username, nombre, rol FROM usuarios";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id_usuario", rs.getInt("id_usuario"));
                obj.addProperty("username", rs.getString("username"));
                obj.addProperty("nombre", rs.getString("nombre"));
                obj.addProperty("rol", rs.getString("rol"));
                lista.add(obj);
            }
        }
        return lista;
    }
}
