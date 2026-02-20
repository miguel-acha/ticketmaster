package edu.upb.tickmaster.server.repositories;

import edu.upb.tickmaster.db.ConexionDb;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TicketRepository {

    public TicketRepository() {
    }

    public void saveTicket(String ticketId, int eventId, String userName, String seatNumber, String idempotencyKey)
            throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "INSERT INTO tickets (id, event_id, user_name, seat_number, purchase_date, idempotency_key) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ticketId);
            pstmt.setInt(2, eventId);
            pstmt.setString(3, userName);
            pstmt.setString(4, seatNumber);
            pstmt.setString(5, new java.util.Date().toString());
            pstmt.setString(6, idempotencyKey);
            pstmt.executeUpdate();
        }
    }

    public void saveEvent(String name, int totalTickets, String date) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "INSERT INTO events (name, total_tickets, date) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, totalTickets);
            pstmt.setString(3, date);
            pstmt.executeUpdate();
        }
    }

    public void incrementSoldTickets(int eventId) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "UPDATE events SET sold_tickets = sold_tickets + 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.executeUpdate();
        }
    }

    public JsonArray getAllEvents() throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        JsonArray events = new JsonArray();
        String sql = "SELECT * FROM events";

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                JsonObject event = new JsonObject();
                event.addProperty("id", rs.getInt("id"));
                event.addProperty("name", rs.getString("name"));
                event.addProperty("total_tickets", rs.getInt("total_tickets"));
                event.addProperty("sold_tickets", rs.getInt("sold_tickets"));
                event.addProperty("date", rs.getString("date"));
                events.add(event);
            }
        }
        return events;
    }

    public JsonObject findByIdempotencyKey(String key) throws SQLException {
        Connection conn = ConexionDb.getInstance().getConnection();
        String sql = "SELECT id, event_id, user_name, seat_number FROM tickets WHERE idempotency_key = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.addProperty("status", "OK");
                    jsonResponse.addProperty("ticket_id", rs.getString("id"));
                    jsonResponse.addProperty("message", "Ticket purchased successfully");
                    return jsonResponse;
                }
            }
        }
        return null;
    }
}
