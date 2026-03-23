package edu.upb.tickmaster.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;

public class ConexionDb {

    private static final Logger logger = LoggerFactory.getLogger(ConexionDb.class);
    private static final String DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
    private static final String DB_PORT = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
    private static final String DB_NAME = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "tickmaster";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "1711";

    private static final String SERVER_URL = "jdbc:mariadb://" + DB_HOST + ":" + DB_PORT + "/";

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
            connection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
        } catch (SQLException e) {
            logger.error("Error al conectar al servidor MariaDB", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // Crear base de datos si no existe
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.execute("USE " + DB_NAME);

            // -------------------------------------------------------
            // 1. Tabla USUARIOS
            // -------------------------------------------------------
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios ("
                    + "  id_usuario INT PRIMARY KEY AUTO_INCREMENT,"
                    + "  username   VARCHAR(100) NOT NULL UNIQUE,"
                    + "  nombre     VARCHAR(255) NOT NULL,"
                    + "  password   VARCHAR(255) NOT NULL,"
                    + "  rol        ENUM('admin','cliente') DEFAULT 'cliente'"
                    + ")");

            // -------------------------------------------------------
            // 2. Tabla EVENTOS
            // -------------------------------------------------------
            stmt.execute("CREATE TABLE IF NOT EXISTS eventos ("
                    + "  id_evento   INT PRIMARY KEY AUTO_INCREMENT,"
                    + "  nombre      VARCHAR(255) NOT NULL,"
                    + "  fecha       DATETIME NOT NULL,"
                    + "  capacidad   INT NOT NULL,"
                    + "  imagen_url  VARCHAR(500)"
                    + ")");

            // -------------------------------------------------------
            // 3. Tabla TIPO_TICKET
            // -------------------------------------------------------
            stmt.execute("CREATE TABLE IF NOT EXISTS tipo_ticket ("
                    + "  id_tipo_ticket INT PRIMARY KEY AUTO_INCREMENT,"
                    + "  id_evento      INT NOT NULL,"
                    + "  tipo_asiento   VARCHAR(100) NOT NULL,"
                    + "  cantidad       INT NOT NULL,"
                    + "  precio         DECIMAL(10,2) NOT NULL,"
                    + "  FOREIGN KEY (id_evento) REFERENCES eventos(id_evento)"
                    + ")");

            // -------------------------------------------------------
            // 4. Tabla TICKETS
            // -------------------------------------------------------
            stmt.execute("CREATE TABLE IF NOT EXISTS tickets ("
                    + "  id_ticket       VARCHAR(36) PRIMARY KEY,"
                    + "  id_evento       INT NOT NULL,"
                    + "  id_usuario      INT NOT NULL,"
                    + "  nro_asiento     VARCHAR(50),"
                    + "  precio          DECIMAL(10,2) NOT NULL,"
                    + "  idempotency_key VARCHAR(255) UNIQUE,"
                    + "  id_tipo_ticket  INT NOT NULL,"
                    + "  FOREIGN KEY (id_evento)      REFERENCES eventos(id_evento),"
                    + "  FOREIGN KEY (id_usuario)     REFERENCES usuarios(id_usuario),"
                    + "  FOREIGN KEY (id_tipo_ticket) REFERENCES tipo_ticket(id_tipo_ticket)"
                    + ")");

            // -------------------------------------------------------
            // 5. Tabla COMPRAS
            // -------------------------------------------------------
            stmt.execute("CREATE TABLE IF NOT EXISTS compras ("
                    + "  id_compra    INT PRIMARY KEY AUTO_INCREMENT,"
                    + "  id_ticket    VARCHAR(36) NOT NULL,"
                    + "  id_usuario   INT NOT NULL,"
                    + "  orden_id     VARCHAR(100),"
                    + "  fecha_compra DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "  total        DECIMAL(10,2) NOT NULL,"
                    + "  estado       ENUM('pendiente','completada','cancelada') DEFAULT 'completada',"
                    + "  FOREIGN KEY (id_ticket)  REFERENCES tickets(id_ticket),"
                    + "  FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)"
                    + ")");

            // -------------------------------------------------------
            // 6. Tabla PASARELADEPAGO_COBROS
            // -------------------------------------------------------
            stmt.execute("CREATE TABLE IF NOT EXISTS pasareladepago_cobros ("
                    + "  id_cobro    INT PRIMARY KEY AUTO_INCREMENT,"
                    + "  id_usuario  INT NOT NULL,"
                    + "  monto       DECIMAL(10,2) NOT NULL,"
                    + "  estado      ENUM('pendiente','completado','fallido') DEFAULT 'completado',"
                    + "  fecha_cobro DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "  FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)"
                    + ")");

            // -------------------------------------------------------
            // 7. Tabla PASARELADEPAGO_NOTIFICACIONES
            // -------------------------------------------------------
            stmt.execute("CREATE TABLE IF NOT EXISTS pasareladepago_notificaciones ("
                    + "  id_notificacion INT PRIMARY KEY AUTO_INCREMENT,"
                    + "  id_cobro        INT NOT NULL,"
                    + "  mensaje         VARCHAR(255) NOT NULL,"
                    + "  enviada         BOOLEAN DEFAULT TRUE,"
                    + "  fecha_notif     DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "  FOREIGN KEY (id_cobro) REFERENCES pasareladepago_cobros(id_cobro)"
                    + ")");

            // Datos iniciales
            seedData(stmt);

            // Migracion: agregar orden_id a compras si no existe
            try {
                stmt.execute("ALTER TABLE compras ADD COLUMN orden_id VARCHAR(100)");
                logger.info("Columna orden_id agregada a compras.");
            } catch (SQLException ignored) {
            }

            // Migracion: agregar imagen_url a eventos si no existe
            try {
                stmt.execute("ALTER TABLE eventos ADD COLUMN imagen_url VARCHAR(500)");
                logger.info("Columna imagen_url agregada a eventos.");
            } catch (SQLException ignored) {
            }

            logger.info("Esquema de BD creado incluyendo pasareladepago.");

        } catch (SQLException e) {
            logger.error("Error al crear las tablas o base de datos", e);
            throw e;
        }
    }

    private void seedData(Statement stmt) throws SQLException {
        // Usuarios de prueba
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios")) {
            if (rs.next() && rs.getInt(1) == 0) {
                String hashAdmin = BCrypt.hashpw("admin123", BCrypt.gensalt());
                String hashCliente = BCrypt.hashpw("1234", BCrypt.gensalt());
                stmt.execute("INSERT INTO usuarios (username, nombre, password, rol) "
                        + "VALUES ('admin', 'Administrador', '" + hashAdmin + "', 'admin')");
                stmt.execute("INSERT INTO usuarios (username, nombre, password, rol) "
                        + "VALUES ('cliente1', 'Juan Perez', '" + hashCliente + "', 'cliente')");
                logger.info("Usuarios iniciales creados.");
            }
        }

        // Eventos de prueba
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM eventos")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO eventos (nombre, fecha, capacidad) "
                        + "VALUES ('Concierto UPB', '2025-06-01 20:00:00', 200)");
                stmt.execute("INSERT INTO eventos (nombre, fecha, capacidad) "
                        + "VALUES ('Festival de Jazz', '2025-08-15 18:00:00', 500)");

                // Tipos de ticket - Concierto UPB (id=1)
                stmt.execute("INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio) "
                        + "VALUES (1, 'VIP', 50, 150.00)");
                stmt.execute("INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio) "
                        + "VALUES (1, 'General', 150, 50.00)");

                // Tipos de ticket - Festival de Jazz (id=2)
                stmt.execute("INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio) "
                        + "VALUES (2, 'Palco', 100, 200.00)");
                stmt.execute("INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio) "
                        + "VALUES (2, 'General', 400, 75.00)");

                logger.info("Eventos y tipos de ticket iniciales creados.");
            }
        }
    }

    /** Obtener la única instancia (Singleton). */
    public static ConexionDb getInstance() {
        return db;
    }

    /** Obtener la conexión activa. */
    public Connection getConnection() {
        return connection;
    }

    /** Cerrar la conexión. */
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

    public static void main(String[] args) {
        ConexionDb.getInstance();
    }
}
