package core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.File;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:db/library.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        // Ensure db directory exists
        new File("db").mkdirs();

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            // Core Entities
            stmt.execute("CREATE TABLE IF NOT EXISTS roles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "role_name TEXT UNIQUE NOT NULL)");
                    
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "phone TEXT, " +
                    "password_hash TEXT NOT NULL, " +
                    "role_id INTEGER, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(role_id) REFERENCES roles(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS authors (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL)");
                    
            stmt.execute("CREATE TABLE IF NOT EXISTS publishers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id TEXT PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "isbn TEXT UNIQUE, " +
                    "author_id INTEGER, " +
                    "category_id INTEGER, " +
                    "publisher_id INTEGER, " +
                    "cover_image_url TEXT, " +
                    "description TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(author_id) REFERENCES authors(id), " +
                    "FOREIGN KEY(category_id) REFERENCES categories(id), " +
                    "FOREIGN KEY(publisher_id) REFERENCES publishers(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS book_copies (" +
                    "id TEXT PRIMARY KEY, " +
                    "book_id TEXT NOT NULL, " +
                    "barcode TEXT UNIQUE, " +
                    "status TEXT DEFAULT 'AVAILABLE', " + // AVAILABLE, BORROWED, LOST, DAMAGED
                    "condition TEXT DEFAULT 'GOOD', " +
                    "shelf_location TEXT, " +
                    "FOREIGN KEY(book_id) REFERENCES books(id))");

            // Transactions & Financials
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id TEXT PRIMARY KEY, " +
                    "copy_id TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "issue_date DATETIME NOT NULL, " +
                    "due_date DATETIME NOT NULL, " +
                    "return_date DATETIME, " +
                    "status TEXT DEFAULT 'ACTIVE', " + // ACTIVE, RETURNED, OVERDUE
                    "FOREIGN KEY(copy_id) REFERENCES book_copies(id), " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS fines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "transaction_id TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "reason TEXT, " +
                    "status TEXT DEFAULT 'UNPAID', " + // UNPAID, PAID
                    "FOREIGN KEY(transaction_id) REFERENCES transactions(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS payments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "fine_id INTEGER NOT NULL, " +
                    "amount_paid REAL NOT NULL, " +
                    "payment_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(fine_id) REFERENCES fines(id))");

            // Requests & Workflows
            stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "reservation_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "status TEXT DEFAULT 'PENDING', " + // PENDING, FULFILLED, CANCELLED
                    "FOREIGN KEY(book_id) REFERENCES books(id), " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS renewal_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "transaction_id TEXT NOT NULL, " +
                    "request_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "status TEXT DEFAULT 'PENDING', " + // PENDING, APPROVED, REJECTED
                    "FOREIGN KEY(transaction_id) REFERENCES transactions(id))");

            // System Logs & Settings
            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "is_read BOOLEAN DEFAULT 0, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(user_id) REFERENCES users(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id TEXT, " +
                    "action TEXT NOT NULL, " +
                    "details TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS library_settings (" +
                    "setting_key TEXT PRIMARY KEY, " +
                    "setting_value TEXT NOT NULL)");

            System.out.println("Database schema initialized successfully.");
            
            // Insert default roles if empty
            stmt.execute("INSERT OR IGNORE INTO roles (id, role_name) VALUES (1, 'ADMIN'), (2, 'LIBRARIAN'), (3, 'USER')");
            
            // Insert the main custom user requested by the user
            stmt.execute("INSERT OR IGNORE INTO users (id, name, email, password_hash, role_id) VALUES ('U0', 'Shamil', 'shamil@example.com', 'Shamil@123', 1)");
            
            // Update admin email to admin@example.com just in case it was 'Shamil' before
            stmt.execute("UPDATE users SET email = 'admin@example.com' WHERE id = 'U1'");
            
            // Insert default admin if not exists (password: admin123)
            stmt.execute("INSERT OR IGNORE INTO users (id, name, email, password_hash, role_id) VALUES ('U1', 'System Admin', 'admin@example.com', 'admin123', 1)");
            
            // Insert default user
            stmt.execute("INSERT OR IGNORE INTO users (id, name, email, password_hash, role_id) VALUES ('U2', 'Test Student', 'student', 'student123', 3)");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database schema.");
        }
    }
}
