package core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.File;

public class DatabaseManager {
    private static String DB_URL = "jdbc:sqlite:db/library.db";
    private static boolean isPostgres = false;

    static {
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl != null && !envDbUrl.isEmpty()) {
            DB_URL = envDbUrl;
            isPostgres = true;
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static boolean isPostgres() {
        return isPostgres;
    }

    public static void initializeDatabase() {
        if (!isPostgres) {
            new File("db").mkdirs();
        }

        String autoInc = isPostgres ? "SERIAL PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String dateTime = isPostgres ? "TIMESTAMP" : "DATETIME";
        String boolType = isPostgres ? "BOOLEAN" : "BOOLEAN";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            System.out.println("Starting Database Schema Initialization...");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS roles (id " + autoInc + ", role_name VARCHAR(50) UNIQUE NOT NULL)");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS users (id VARCHAR(50) PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(100) UNIQUE NOT NULL, phone VARCHAR(20), password_hash VARCHAR(255) NOT NULL, role_id INTEGER, created_at " + dateTime + " DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(role_id) REFERENCES roles(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS authors (id " + autoInc + ", name VARCHAR(100) UNIQUE NOT NULL)");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS categories (id " + autoInc + ", name VARCHAR(100) UNIQUE NOT NULL)");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS publishers (id " + autoInc + ", name VARCHAR(100) UNIQUE NOT NULL)");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS books (id VARCHAR(50) PRIMARY KEY, title VARCHAR(255) NOT NULL, isbn VARCHAR(50) UNIQUE, author_id INTEGER, category_id INTEGER, publisher_id INTEGER, cover_image_url VARCHAR(500), description TEXT, created_at " + dateTime + " DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(author_id) REFERENCES authors(id), FOREIGN KEY(category_id) REFERENCES categories(id), FOREIGN KEY(publisher_id) REFERENCES publishers(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS book_copies (id VARCHAR(50) PRIMARY KEY, book_id VARCHAR(50) NOT NULL, barcode VARCHAR(100) UNIQUE, status VARCHAR(50) DEFAULT 'AVAILABLE', condition VARCHAR(50) DEFAULT 'GOOD', shelf_location VARCHAR(100), FOREIGN KEY(book_id) REFERENCES books(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS transactions (id VARCHAR(50) PRIMARY KEY, copy_id VARCHAR(50) NOT NULL, user_id VARCHAR(50) NOT NULL, issue_date " + dateTime + " NOT NULL, due_date " + dateTime + " NOT NULL, return_date " + dateTime + ", status VARCHAR(50) DEFAULT 'ACTIVE', FOREIGN KEY(copy_id) REFERENCES book_copies(id), FOREIGN KEY(user_id) REFERENCES users(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS fines (id " + autoInc + ", transaction_id VARCHAR(50) NOT NULL, amount REAL NOT NULL, reason TEXT, status VARCHAR(50) DEFAULT 'UNPAID', FOREIGN KEY(transaction_id) REFERENCES transactions(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS payments (id " + autoInc + ", fine_id INTEGER NOT NULL, amount_paid REAL NOT NULL, payment_date " + dateTime + " DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(fine_id) REFERENCES fines(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS reservations (id " + autoInc + ", book_id VARCHAR(50) NOT NULL, user_id VARCHAR(50) NOT NULL, reservation_date " + dateTime + " DEFAULT CURRENT_TIMESTAMP, status VARCHAR(50) DEFAULT 'PENDING', FOREIGN KEY(book_id) REFERENCES books(id), FOREIGN KEY(user_id) REFERENCES users(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS renewal_requests (id " + autoInc + ", transaction_id VARCHAR(50) NOT NULL, request_date " + dateTime + " DEFAULT CURRENT_TIMESTAMP, status VARCHAR(50) DEFAULT 'PENDING', FOREIGN KEY(transaction_id) REFERENCES transactions(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS notifications (id " + autoInc + ", user_id VARCHAR(50) NOT NULL, message TEXT NOT NULL, is_read " + boolType + " DEFAULT " + (isPostgres ? "false" : "0") + ", created_at " + dateTime + " DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user_id) REFERENCES users(id))");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS audit_logs (id " + autoInc + ", user_id VARCHAR(50), action VARCHAR(100) NOT NULL, details TEXT, created_at " + dateTime + " DEFAULT CURRENT_TIMESTAMP)");
            executeSql(stmt, "CREATE TABLE IF NOT EXISTS library_settings (setting_key VARCHAR(100) PRIMARY KEY, setting_value TEXT NOT NULL)");

            System.out.println("Database schema initialized successfully.");
            
            // Safe inserts that work on both by catching exceptions if they already exist
            safeInsert(stmt, "INSERT INTO roles (id, role_name) VALUES (1, 'ADMIN')");
            safeInsert(stmt, "INSERT INTO roles (id, role_name) VALUES (2, 'LIBRARIAN')");
            safeInsert(stmt, "INSERT INTO roles (id, role_name) VALUES (3, 'USER')");
            
            safeInsert(stmt, "INSERT INTO users (id, name, email, password_hash, role_id) VALUES ('U0', 'Shamil', 'shamil@example.com', 'Shamil@123', 1)");
            safeInsert(stmt, "INSERT INTO users (id, name, email, password_hash, role_id) VALUES ('U1', 'System Admin', 'admin@example.com', 'admin123', 1)");
            safeInsert(stmt, "INSERT INTO users (id, name, email, password_hash, role_id) VALUES ('U2', 'Test Student', 'student', 'student123', 3)");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database schema.");
        }
    }

    private static void executeSql(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("SQL Execution Failed for: " + sql);
            e.printStackTrace();
        }
    }
    
    private static void safeInsert(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            // Ignore duplicate key constraints
        }
    }
}
