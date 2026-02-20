package edu.upb.tickmaster.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDb {

    private static final Logger logger = LoggerFactory.getLogger(ConexionDb.class);
    private static final String DB_URL = "jdbc:sqlite:tickmaster.db";
    private static ConexionDb db;
    private Connection connection;

    static {
        db = new ConexionDb();
        try {

            db.createTables(); // Ensure the tables exist when the connection is created
            logger.info("Conexion a la base de datos establecida.");
        } catch (SQLException e) {
            logger.error("Error al conectar a la base de datos", e);
        }
        logger.info("Iniciando Singleton...");
    }

    // Constructor privado para Singleton
    private ConexionDb() {

        try {
            connection = DriverManager.getConnection(DB_URL);

        } catch (SQLException e) {
            logger.error("Error al conectar a la base de datos", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create EVENTS table
            String sqlEvents = "CREATE TABLE IF NOT EXISTS events ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL,"
                    + " total_tickets INTEGER NOT NULL,"
                    + " sold_tickets INTEGER DEFAULT 0,"
                    + " date TEXT"
                    + ");";
            stmt.execute(sqlEvents);

            // Create TICKETS table
            String sqlTickets = "CREATE TABLE IF NOT EXISTS tickets ("
                    + " id TEXT PRIMARY KEY,"
                    + " event_id INTEGER NOT NULL,"
                    + " user_name TEXT NOT NULL,"
                    + " seat_number TEXT,"
                    + " purchase_date TEXT,"
                    + " idempotency_key TEXT UNIQUE,"
                    + " FOREIGN KEY (event_id) REFERENCES events(id)"
                    + ");";
            stmt.execute(sqlTickets);

            // Seed initial event if empty
            String checkEvent = "SELECT COUNT(*) FROM events";
            if (stmt.executeQuery(checkEvent).getInt(1) == 0) {
                stmt.execute(
                        "INSERT INTO events (name, total_tickets, date) VALUES ('Concierto UPB', 100, '2024-12-01')");
                logger.info("Evento inicial creado.");
            }

            logger.info("Tablas 'events' y 'tickets' verificadas/creadas exitosamente.");
        } catch (SQLException e) {
            logger.error("Error al crear las tablas", e);
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
