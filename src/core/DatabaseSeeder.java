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
            for (String cat : categories) {
                PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO categories (name) VALUES (?)");
                ps.setString(1, cat);
                ps.executeUpdate();
            }

            String[] authors = {"Robert C. Martin", "Isaac Asimov", "Yuval Noah Harari", "Plato", "Carl Sagan", "J.K. Rowling", "George Orwell", "Jane Austen", "Stephen Hawking"};
            for (String author : authors) {
                PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO authors (name) VALUES (?)");
                ps.setString(1, author);
                ps.executeUpdate();
            }

            String[] publishers = {"O'Reilly Media", "Penguin Random House", "HarperCollins", "Simon & Schuster", "Macmillan"};
            for (String pub : publishers) {
                PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO publishers (name) VALUES (?)");
                ps.setString(1, pub);
                ps.executeUpdate();
            }

            Random rand = new Random();
            
            // Generate 100+ Books and 150+ Copies
            String[] adjectives = {"The Quantum", "Advanced", "Introduction to", "The History of", "Mastering", "Modern", "Classic", "The Art of", "Understanding", "Deep", "Applied", "Essential", "Fundamentals of", "Principles of", "The Science of", "A Guide to", "Exploring", "The Complete", "Practical", "Interactive"};
            String[] nouns = {"Algorithms", "Empire", "Universe", "Design Patterns", "Society", "Code", "Mind", "Machine Learning", "Physics", "Civilization", "Data Structures", "Networks", "Databases", "Artificial Intelligence", "Robotics", "Psychology", "Economics", "Philosophy", "Chemistry", "Biology", "Management"};
            
            int copiesCount = 0;
            for (int i = 1; i <= 500; i++) {
                String id = "B" + String.format("%04d", i);
                String title = adjectives[rand.nextInt(adjectives.length)] + " " + nouns[rand.nextInt(nouns.length)];
                String isbn = "978-" + rand.nextInt(900) + "-" + rand.nextInt(90000) + "-" + rand.nextInt(9);
                int authorId = rand.nextInt(authors.length) + 1;
                int categoryId = rand.nextInt(categories.length) + 1;
                int publisherId = rand.nextInt(publishers.length) + 1;
                
                // Assign a variety of vibrant cover colors
                String[] colors = {"#1e3a8a", "#064e3b", "#701a75", "#7c2d12", "#0f172a", "#831843", "#14532d", "#312e81"};
                String color1 = colors[rand.nextInt(colors.length)];
                String color2 = colors[rand.nextInt(colors.length)];
                String cover = "linear-gradient(135deg, " + color1 + ", " + color2 + ")";
                
                PreparedStatement ps = conn.prepareStatement("INSERT INTO books (id, title, isbn, author_id, category_id, publisher_id, cover_image_url, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, title);
                ps.setString(3, isbn);
                ps.setInt(4, authorId);
                ps.setInt(5, categoryId);
                ps.setInt(6, publisherId);
                ps.setString(7, cover);
                ps.setString(8, "A comprehensive and engaging book exploring the concepts of " + title.toLowerCase() + ".");
                ps.executeUpdate();

                // Generate 1 to 5 copies for each book (ensuring ~1500 copies)
                int numCopies = rand.nextInt(5) + 1;
                for (int c = 1; c <= numCopies; c++) {
                    copiesCount++;
                    String copyId = "C" + String.format("%04d", copiesCount);
                    String barcode = "BC" + (100000 + rand.nextInt(900000));
                    
                    PreparedStatement pcs = conn.prepareStatement("INSERT INTO book_copies (id, book_id, barcode, status, shelf_location) VALUES (?, ?, ?, ?, ?)");
                    pcs.setString(1, copyId);
                    pcs.setString(2, id);
                    pcs.setString(3, barcode);
                    pcs.setString(4, "AVAILABLE");
                    pcs.setString(5, "Shelf-" + (char)(rand.nextInt(26) + 'A') + "-" + (rand.nextInt(20)+1));
                    pcs.executeUpdate();
                }
            }

            // Generate 150 Users
            String[] firstNames = {"John", "Emma", "Michael", "Sophia", "William", "Olivia", "James", "Ava", "Alexander", "Isabella", "Daniel", "Mia", "Matthew", "Charlotte", "David", "Amelia", "Joseph", "Harper", "Samuel", "Evelyn", "Henry", "Abigail", "Jackson", "Emily", "Sebastian", "Elizabeth", "Aiden", "Mila", "Matthew", "Ella"};
            String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson"};
            
            for (int i = 1; i <= 150; i++) {
                String id = "U" + (i + 10);
                String name = firstNames[rand.nextInt(firstNames.length)] + " " + lastNames[rand.nextInt(lastNames.length)];
                String email = name.toLowerCase().replace(" ", ".") + "@example.com";
                
                PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO users (id, name, email, password_hash, role_id) VALUES (?, ?, ?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, "password123");
                ps.setInt(5, 3); // USER role
                ps.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("Seeding completed successfully! Total Books: 500, Copies: " + copiesCount + ", Users: 150.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Seeding failed.");
        }
    }
}
