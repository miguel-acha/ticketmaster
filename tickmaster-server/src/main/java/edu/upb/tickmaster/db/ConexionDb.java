package edu.upb.tickmaster.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDb {

    private static final Logger logger = LoggerFactory.getLogger(ConexionDb.class);
    private static final String SERVER_URL = "jdbc:mariadb://localhost:3306/";
    private static final String DB_NAME = "tickmaster";
    private static final String USER = "root";
    private static final String PASSWORD = "1711";

    private static ConexionDb db;
    private Connection connection;

    static {
        db = new ConexionDb();
        try {
            db.initializeDatabase();
            logger.info("Conexion a la base de datos MariaDB establecida.");
        } catch (SQLException e) {
            logger.error("Error al inicializar la base de datos", e);
        }
    }

    private ConexionDb() {
        try {
            // Initial connection to the server to ensure DB exists
            connection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
        } catch (SQLException e) {
            logger.error("Error al conectar al servidor MariaDB", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create database if not exists
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.execute("USE " + DB_NAME);

            // Create EVENTS table
            String sqlEvents = "CREATE TABLE IF NOT EXISTS events ("
                    + " id INT PRIMARY KEY AUTO_INCREMENT,"
                    + " name VARCHAR(255) NOT NULL,"
                    + " total_tickets INT NOT NULL,"
                    + " sold_tickets INT DEFAULT 0,"
                    + " event_date VARCHAR(50)"
                    + ");";
            stmt.execute(sqlEvents);

            // Create TICKETS table
            String sqlTickets = "CREATE TABLE IF NOT EXISTS tickets ("
                    + " id VARCHAR(255) PRIMARY KEY,"
                    + " event_id INT NOT NULL,"
                    + " user_name VARCHAR(255) NOT NULL,"
                    + " seat_number VARCHAR(50),"
                    + " purchase_date VARCHAR(100),"
                    + " idempotency_key VARCHAR(255) UNIQUE,"
                    + " FOREIGN KEY (event_id) REFERENCES events(id)"
                    + ");";
            stmt.execute(sqlTickets);

            // Seed initial event if empty
            String checkEvent = "SELECT COUNT(*) FROM events";
            try (ResultSet rs = stmt.executeQuery(checkEvent)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute(
                            "INSERT INTO events (name, total_tickets, event_date) VALUES ('Concierto UPB', 100, '2024-12-01')");
                    logger.info("Evento inicial creado.");
                }
            }

            logger.info("Tablas 'events' y 'tickets' verificadas/creadas exitosamente en MariaDB.");
        } catch (SQLException e) {
            logger.error("Error al crear las tablas o base de datos", e);
            throw e;
        }
    }

    // Método para obtener la única instancia
    public static ConexionDb getInstance() {
        return db;
    }

    // Método para obtener la conexión
    public Connection getConnection() {
        return connection;
    }

    // Método para cerrar la conexión
    public void cerrarConexion() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Conexion cerrada correctamente.");
            } catch (SQLException e) {
                logger.error("Error al cerrar la conexión", e);
            }
        }
    }

    // Main method for testing connection
    public static void main(String[] args) {
        ConexionDb.getInstance();
    }
}
