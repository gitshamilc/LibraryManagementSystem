package core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;
import java.time.LocalDate;

public class DatabaseSeeder {

    public static void seed() {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Check if already seeded
            var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM books");
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Database already seeded. Skipping.");
                return;
            }

            System.out.println("Seeding database with realistic data...");
            conn.setAutoCommit(false);

            String[] categories = {"Technology", "Science Fiction", "History", "Philosophy", "Mathematics", "Art", "Literature", "Biography"};
            String insertOrIgnore = DatabaseManager.isPostgres() ? "ON CONFLICT DO NOTHING" : "OR IGNORE";
            String catSql = DatabaseManager.isPostgres() ? "INSERT INTO categories (name) VALUES (?) ON CONFLICT (name) DO NOTHING" : "INSERT OR IGNORE INTO categories (name) VALUES (?)";
            for (String cat : categories) {
                PreparedStatement ps = conn.prepareStatement(catSql);
                ps.setString(1, cat);
                ps.executeUpdate();
            }

            String[] authors = {"Robert C. Martin", "Isaac Asimov", "Yuval Noah Harari", "Plato", "Carl Sagan", "J.K. Rowling", "George Orwell", "Jane Austen", "Stephen Hawking"};
            String authSql = DatabaseManager.isPostgres() ? "INSERT INTO authors (name) VALUES (?) ON CONFLICT (name) DO NOTHING" : "INSERT OR IGNORE INTO authors (name) VALUES (?)";
            for (String author : authors) {
                PreparedStatement ps = conn.prepareStatement(authSql);
                ps.setString(1, author);
                ps.executeUpdate();
            }

            String[] publishers = {"O'Reilly Media", "Penguin Random House", "HarperCollins", "Simon & Schuster", "Macmillan"};
            String pubSql = DatabaseManager.isPostgres() ? "INSERT INTO publishers (name) VALUES (?) ON CONFLICT (name) DO NOTHING" : "INSERT OR IGNORE INTO publishers (name) VALUES (?)";
            for (String pub : publishers) {
                PreparedStatement ps = conn.prepareStatement(pubSql);
                ps.setString(1, pub);
                ps.executeUpdate();
            }

            Random rand = new Random();
            
            // Generate 100+ Books and 150+ Copies
            String[] adjectives = {"The Quantum", "Advanced", "Introduction to", "The History of", "Mastering", "Modern", "Classic", "The Art of", "Understanding", "Deep", "Applied", "Essential", "Fundamentals of", "Principles of", "The Science of", "A Guide to", "Exploring", "The Complete", "Practical", "Interactive"};
            String[] nouns = {"Algorithms", "Empire", "Universe", "Design Patterns", "Society", "Code", "Mind", "Machine Learning", "Physics", "Civilization", "Data Structures", "Networks", "Databases", "Artificial Intelligence", "Robotics", "Psychology", "Economics", "Philosophy", "Chemistry", "Biology", "Management"};
            
            int copiesCount = 0;
            String bookSql = DatabaseManager.isPostgres() ? "INSERT INTO books (id, title, isbn, author_id, category_id, publisher_id, cover_image_url, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING" : "INSERT OR IGNORE INTO books (id, title, isbn, author_id, category_id, publisher_id, cover_image_url, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            for (int i = 1; i <= 100; i++) { // Reduced to 100 to prevent Supabase connection timeouts
                String id = "B" + String.format("%04d", i);
                String title = adjectives[rand.nextInt(adjectives.length)] + " " + nouns[rand.nextInt(nouns.length)];
                String isbn = "978-" + rand.nextInt(900) + "-" + rand.nextInt(90000) + "-" + rand.nextInt(9);
                int authorId = rand.nextInt(authors.length) + 1;
                int categoryId = rand.nextInt(categories.length) + 1;
                int publisherId = rand.nextInt(publishers.length) + 1;
                
                String[] colors = {"#1e3a8a", "#064e3b", "#701a75", "#7c2d12", "#0f172a", "#831843", "#14532d", "#312e81"};
                String color1 = colors[rand.nextInt(colors.length)];
                String color2 = colors[rand.nextInt(colors.length)];
                String cover = "linear-gradient(135deg, " + color1 + ", " + color2 + ")";
                
                PreparedStatement ps = conn.prepareStatement(bookSql);
                ps.setString(1, id);
                ps.setString(2, title);
                ps.setString(3, isbn);
                ps.setInt(4, authorId);
                ps.setInt(5, categoryId);
                ps.setInt(6, publisherId);
                ps.setString(7, cover);
                ps.setString(8, "A comprehensive and engaging book exploring the concepts of " + title.toLowerCase() + ".");
                ps.executeUpdate();

                int numCopies = rand.nextInt(5) + 1;
                for (int c = 1; c <= numCopies; c++) {
                    copiesCount++;
                    String copyId = "C" + String.format("%04d", copiesCount);
                    String barcode = "BC" + (100000 + rand.nextInt(900000));
                    
                    String copySql = DatabaseManager.isPostgres() ? "INSERT INTO book_copies (id, book_id, barcode, status, shelf_location) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING" : "INSERT OR IGNORE INTO book_copies (id, book_id, barcode, status, shelf_location) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement pcs = conn.prepareStatement(copySql);
                    pcs.setString(1, copyId);
                    pcs.setString(2, id);
                    pcs.setString(3, barcode);
                    pcs.setString(4, "AVAILABLE");
                    pcs.setString(5, "Shelf-" + (char)(rand.nextInt(26) + 'A') + "-" + (rand.nextInt(20)+1));
                    pcs.executeUpdate();
                }
            }

            // Generate Users
            String[] firstNames = {"John", "Emma", "Michael", "Sophia", "William", "Olivia", "James", "Ava"};
            String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"};
            
            String userSql = DatabaseManager.isPostgres() ? "INSERT INTO users (id, name, email, password_hash, role_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (email) DO NOTHING" : "INSERT OR IGNORE INTO users (id, name, email, password_hash, role_id) VALUES (?, ?, ?, ?, ?)";
            
            for (int i = 1; i <= 20; i++) {
                String id = "U" + (i + 10);
                String name = firstNames[rand.nextInt(firstNames.length)] + " " + lastNames[rand.nextInt(lastNames.length)];
                String email = name.toLowerCase().replace(" ", ".") + "@example.com";
                
                PreparedStatement ps = conn.prepareStatement(userSql);
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, "password123");
                ps.setInt(5, 3); // USER role
                ps.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("Seeding completed successfully! Total Books: 100, Copies: " + copiesCount + ", Users: 20.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Seeding failed.");
        }
    }
}
