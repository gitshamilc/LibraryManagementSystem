package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.DatabaseManager;
import dao.UserDAO;
import models.User;
import services.CirculationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApiController implements HttpHandler {
    private final UserDAO userDAO = new UserDAO();
    private final CirculationService circulationService = new CirculationService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if (path.equals("/api/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if (path.equals("/api/signup") && "POST".equalsIgnoreCase(method)) {
                handleSignup(exchange);
            } else if (path.equals("/api/stats") && "GET".equalsIgnoreCase(method)) {
                handleStats(exchange);
            } else if (path.equals("/api/books") && "GET".equalsIgnoreCase(method)) {
                handleGetBooks(exchange);
            } else if (path.equals("/api/books") && "POST".equalsIgnoreCase(method)) {
                handleAddBook(exchange);
            } else if (path.equals("/api/issue") && "POST".equalsIgnoreCase(method)) {
                handleIssue(exchange);
            } else if (path.equals("/api/return") && "POST".equalsIgnoreCase(method)) {
                handleReturn(exchange);
            } else if (path.equals("/api/members") && "GET".equalsIgnoreCase(method)) {
                handleGetMembers(exchange);
            } else if (path.equals("/api/members") && "POST".equalsIgnoreCase(method)) {
                handleAddMember(exchange);
            } else if (path.equals("/api/loans") && "GET".equalsIgnoreCase(method)) {
                handleGetLoans(exchange);
            } else {
                sendJsonResponse(exchange, 404, "{\"error\": \"Endpoint not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{\"error\": \"Internal server error: " + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String email = data.get("email");
        String password = data.get("password");

        if (email == null || email.trim().isEmpty()) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Please enter your email address or User ID.\"}");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Please enter your password.\"}");
            return;
        }

        User user = userDAO.findByIdentifier(email);
        if (user == null) {
            sendJsonResponse(exchange, 401, "{\"success\": false, \"error\": \"Invalid user. No account found with this email or User ID.\"}");
            return;
        }

        if (!user.getPasswordHash().equals(password)) {
            sendJsonResponse(exchange, 401, "{\"success\": false, \"error\": \"Incorrect password. Please verify your password and try again.\"}");
            return;
        }

        String roleName = user.getRoleId() == 1 ? "Administrator" : (user.getRoleId() == 2 ? "Staff Librarian" : "Student Patron");
        String json = String.format("{\"success\": true, \"userId\": \"%s\", \"name\": \"%s\", \"email\": \"%s\", \"roleId\": %d, \"role\": \"%s\"}", 
            user.getId(), escape(user.getName()), escape(user.getEmail()), user.getRoleId(), roleName);
        sendJsonResponse(exchange, 200, json);
    }

    private void handleSignup(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");
        String roleStr = data.get("role");
        String phone = data.get("phone");
        String className = data.get("className");
        String rollNumber = data.get("rollNumber");
        String adminPin = data.get("adminPin");

        // 1. Role determination
        int roleId = 3; // Default: Student Patron
        if ("Staff".equalsIgnoreCase(roleStr) || "Librarian".equalsIgnoreCase(roleStr)) {
            roleId = 2; // Staff Librarian
        } else if ("Admin".equalsIgnoreCase(roleStr)) {
            roleId = 1; // Administrator
        }

        // 2. Name validation
        if (name == null || name.trim().length() < 2) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Full name is required (minimum 2 characters).\"}");
            return;
        }

        // 3. Email format validation
        if (email == null || !email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Invalid email format. Please enter a valid email address (e.g., name@example.com).\"}");
            return;
        }

        // 4. Email existence check
        if (userDAO.emailExists(email)) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"This email is already registered. Please sign in instead.\"}");
            return;
        }

        // 5. Student-specific validations (Mobile number, Class, Roll number required)
        if (roleId == 3) {
            if (phone == null || phone.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Mobile number is required for student registration.\"}");
                return;
            }
            if (className == null || className.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Class / Grade is required for student registration.\"}");
                return;
            }
            if (rollNumber == null || rollNumber.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Roll number is required for student registration.\"}");
                return;
            }
        }

        // 6. Staff security PIN validation (Passcode: 4321)
        if (roleId == 1 || roleId == 2) {
            if (adminPin == null || !"4321".equals(adminPin.trim())) {
                sendJsonResponse(exchange, 403, "{\"success\": false, \"error\": \"Wrong pincode! Please enter the correct PIN.\"}");
                return;
            }
        }

        // 7. Password strength rules
        if (password == null || password.length() < 6) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Password must be at least 6 characters long.\"}");
            return;
        }
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        if (!hasLetter || !hasDigit) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Password is not strong enough. It must contain both letters and numbers.\"}");
            return;
        }

        String prefix = roleId == 1 ? "AD" : (roleId == 2 ? "ST" : "U");
        String newId = prefix + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String cleanPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
        String cleanClass = (className != null && !className.trim().isEmpty()) ? className.trim() : null;
        String cleanRoll = (rollNumber != null && !rollNumber.trim().isEmpty()) ? rollNumber.trim() : null;
        User newUser = new User(newId, name.trim(), email.trim().toLowerCase(), cleanPhone, cleanClass, cleanRoll, password, roleId);

        try {
            boolean success = userDAO.save(newUser);
            if (success) {
                sendJsonResponse(exchange, 200, "{\"success\": true, \"userId\": \"" + newId + "\", \"roleId\": " + roleId + "}");
            } else {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Could not create account. Please try again.\"}");
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\": false, \"error\": \"Registration error: " + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleAddMember(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");
        String roleStr = data.get("role");
        String phone = data.get("phone");
        String className = data.get("className");
        String rollNumber = data.get("rollNumber");
        String adminPin = data.get("adminPin");

        if (name == null || email == null || password == null) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Name, email, and password are required.\"}");
            return;
        }

        int roleId = 3;
        if ("Admin".equalsIgnoreCase(roleStr)) roleId = 1;
        else if ("Librarian".equalsIgnoreCase(roleStr)) roleId = 2;

        String prefix = roleId == 1 ? "AD" : (roleId == 2 ? "ST" : "U");
        String newId = prefix + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String cleanPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
        String cleanClass = (className != null && !className.trim().isEmpty()) ? className.trim() : null;
        String cleanRoll = (rollNumber != null && !rollNumber.trim().isEmpty()) ? rollNumber.trim() : null;
        User newUser = new User(newId, name.trim(), email.trim().toLowerCase(), cleanPhone, cleanClass, cleanRoll, password, roleId);

        try {
            boolean success = userDAO.save(newUser);
            if (success) {
                sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Member created successfully!\", \"userId\": \"" + newId + "\"}");
            } else {
                sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Email already registered.\"}");
            }
        } catch (RuntimeException e) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        int totalBooks = 0;
        int availableBooks = 0;
        int totalCopies = 0;
        int activeMembers = 0;
        int activeLoans = 0;
        int overdueBooks = 0;
        double finesCollected = 0.0;

        try (Connection conn = DatabaseManager.getConnection()) {
            // Total titles
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM books")) {
                if (rs.next()) totalBooks = rs.getInt(1);
            }

            // Total physical copies
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM book_copies")) {
                if (rs.next()) totalCopies = rs.getInt(1);
            }

            // Available physical copies
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM book_copies WHERE status = 'AVAILABLE'")) {
                if (rs.next()) availableBooks = rs.getInt(1);
            }

            // Active members
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) activeMembers = rs.getInt(1);
            }

            // Active loans
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM transactions WHERE status = 'ACTIVE'")) {
                if (rs.next()) activeLoans = rs.getInt(1);
            }

            // Overdue loans (due_date < current date)
            String today = LocalDate.now().toString();
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE status = 'ACTIVE' AND due_date < ?")) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) overdueBooks = rs.getInt(1);
                }
            }

            // Fines
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COALESCE(SUM(amount), 0) FROM fines")) {
                if (rs.next()) finesCollected = rs.getDouble(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        String json = String.format(
            "{\"totalBooks\": %d, \"availableBooks\": %d, \"totalCopies\": %d, \"activeMembers\": %d, \"activeLoans\": %d, \"overdueBooks\": %d, \"finesCollected\": %.2f}",
            totalBooks, availableBooks, totalCopies, activeMembers, activeLoans, overdueBooks, finesCollected
        );
        sendJsonResponse(exchange, 200, json);
    }

    private void handleGetBooks(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        String sql = "SELECT b.id, b.title, b.isbn, b.cover_image_url, b.description, " +
                     "COALESCE(a.name, 'Various Authors') as author_name, " +
                     "COALESCE(c.name, 'General') as category_name, " +
                     "COALESCE((SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id), 0) as total_copies, " +
                     "COALESCE((SELECT COUNT(*) FROM book_copies bc WHERE bc.book_id = b.id AND bc.status = 'AVAILABLE'), 0) as available_copies " +
                     "FROM books b " +
                     "LEFT JOIN authors a ON b.author_id = a.id " +
                     "LEFT JOIN categories c ON b.category_id = c.id " +
                     "ORDER BY b.id ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;

                String id = rs.getString("id");
                String title = rs.getString("title");
                String isbn = rs.getString("isbn");
                String cover = rs.getString("cover_image_url");
                String desc = rs.getString("description");
                String author = rs.getString("author_name");
                String category = rs.getString("category_name");
                int totalCopies = rs.getInt("total_copies");
                int availableCopies = rs.getInt("available_copies");

                sb.append(String.format(
                    "{\"id\":\"%s\",\"title\":\"%s\",\"isbn\":\"%s\",\"author\":\"%s\",\"category\":\"%s\",\"coverUrl\":\"%s\",\"description\":\"%s\",\"totalCopies\":%d,\"availableCopies\":%d}",
                    escape(id), escape(title), escape(isbn), escape(author), escape(category),
                    escape(cover != null ? cover : ""), escape(desc != null ? desc : ""), totalCopies, availableCopies
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleAddBook(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);

        String title = data.get("title");
        String isbn = data.get("isbn");
        String author = data.get("author");
        String category = data.get("category");
        String copiesStr = data.get("copies");

        if (title == null || title.trim().isEmpty()) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Book title is required.\"}");
            return;
        }

        int numCopies = 1;
        try {
            if (copiesStr != null) numCopies = Math.max(1, Integer.parseInt(copiesStr.trim()));
        } catch (NumberFormatException ignored) {}

        if (isbn == null || isbn.trim().isEmpty()) {
            isbn = "978-" + (100 + (int)(Math.random() * 899)) + "-" + (10000 + (int)(Math.random() * 89999)) + "-X";
        }
        if (author == null || author.trim().isEmpty()) author = "Standard Library Author";
        if (category == null || category.trim().isEmpty()) category = "General";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Author
            int authorId = 1;
            PreparedStatement aPs = conn.prepareStatement("SELECT id FROM authors WHERE LOWER(name) = ?");
            aPs.setString(1, author.trim().toLowerCase());
            ResultSet aRs = aPs.executeQuery();
            if (aRs.next()) {
                authorId = aRs.getInt("id");
            } else {
                PreparedStatement insA = conn.prepareStatement("INSERT INTO authors (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
                insA.setString(1, author.trim());
                insA.executeUpdate();
                ResultSet gk = insA.getGeneratedKeys();
                if (gk.next()) authorId = gk.getInt(1);
            }

            // 2. Category
            int categoryId = 1;
            PreparedStatement cPs = conn.prepareStatement("SELECT id FROM categories WHERE LOWER(name) = ?");
            cPs.setString(1, category.trim().toLowerCase());
            ResultSet cRs = cPs.executeQuery();
            if (cRs.next()) {
                categoryId = cRs.getInt("id");
            } else {
                PreparedStatement insC = conn.prepareStatement("INSERT INTO categories (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
                insC.setString(1, category.trim());
                insC.executeUpdate();
                ResultSet gk = insC.getGeneratedKeys();
                if (gk.next()) categoryId = gk.getInt(1);
            }

            // 3. New Book ID
            int count = 1;
            try (ResultSet cntRs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM books")) {
                if (cntRs.next()) count = cntRs.getInt(1) + 1;
            }
            String bookId = "B" + String.format("%04d", count);

            // Cover gradient
            String[] colors = {"#1e3a8a", "#064e3b", "#701a75", "#7c2d12", "#0f172a", "#831843", "#14532d", "#312e81"};
            String color1 = colors[(int)(Math.random() * colors.length)];
            String color2 = colors[(int)(Math.random() * colors.length)];
            String cover = "linear-gradient(135deg, " + color1 + ", " + color2 + ")";
            String desc = "A comprehensive and engaging work on " + title.trim() + " by " + author.trim() + ".";

            PreparedStatement insBook = conn.prepareStatement(
                "INSERT INTO books (id, title, isbn, author_id, category_id, publisher_id, cover_image_url, description) VALUES (?, ?, ?, ?, ?, 1, ?, ?)"
            );
            insBook.setString(1, bookId);
            insBook.setString(2, title.trim());
            insBook.setString(3, isbn.trim());
            insBook.setInt(4, authorId);
            insBook.setInt(5, categoryId);
            insBook.setString(6, cover);
            insBook.setString(7, desc);
            insBook.executeUpdate();

            // 4. Create Physical Copies
            int existingCopies = 0;
            try (ResultSet ecRs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM book_copies")) {
                if (ecRs.next()) existingCopies = ecRs.getInt(1);
            }

            for (int i = 1; i <= numCopies; i++) {
                String copyId = "C" + String.format("%04d", existingCopies + i);
                String barcode = "BC" + (100000 + (int)(Math.random() * 899999));
                PreparedStatement insCopy = conn.prepareStatement(
                    "INSERT INTO book_copies (id, book_id, barcode, status, condition, shelf_location) VALUES (?, ?, ?, 'AVAILABLE', 'GOOD', ?)"
                );
                insCopy.setString(1, copyId);
                insCopy.setString(2, bookId);
                insCopy.setString(3, barcode);
                insCopy.setString(4, "Shelf-" + (char)('A' + (int)(Math.random() * 8)) + "-" + (1 + (int)(Math.random() * 12)));
                insCopy.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);

            sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Book added successfully with " + numCopies + " copy(ies)!\", \"bookId\": \"" + bookId + "\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{\"success\": false, \"error\": \"Database error: " + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleGetLoans(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        String sql = "SELECT t.id, t.copy_id, t.user_id, t.issue_date, t.due_date, t.return_date, t.status, " +
                     "COALESCE(u.name, t.user_id) as user_name, " +
                     "COALESCE(u.email, t.user_id) as user_email, " +
                     "COALESCE(b.id, t.copy_id) as book_id, " +
                     "COALESCE(b.title, t.copy_id) as book_title " +
                     "FROM transactions t " +
                     "LEFT JOIN book_copies c ON t.copy_id = c.id " +
                     "LEFT JOIN books b ON c.book_id = b.id " +
                     "LEFT JOIN users u ON (t.user_id = u.id OR LOWER(t.user_id) = LOWER(u.email)) " +
                     "ORDER BY (CASE WHEN t.status = 'ACTIVE' THEN 1 ELSE 2 END), t.issue_date DESC " +
                     "LIMIT 100";

        LocalDate today = LocalDate.now();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;

                String transId = rs.getString("id");
                String copyId = rs.getString("copy_id");
                String userId = rs.getString("user_id");
                String issueDate = rs.getString("issue_date");
                String dueDateStr = rs.getString("due_date");
                String returnDate = rs.getString("return_date");
                String status = rs.getString("status");
                String userName = rs.getString("user_name");
                String userEmail = rs.getString("user_email");
                String bookId = rs.getString("book_id");
                String bookTitle = rs.getString("book_title");

                boolean isOverdue = false;
                long daysOverdue = 0;
                double fine = 0.0;

                if ("ACTIVE".equalsIgnoreCase(status) && dueDateStr != null) {
                    try {
                        LocalDate due = LocalDate.parse(dueDateStr.length() > 10 ? dueDateStr.substring(0, 10) : dueDateStr);
                        if (today.isAfter(due)) {
                            isOverdue = true;
                            daysOverdue = ChronoUnit.DAYS.between(due, today);
                            fine = daysOverdue * 5.0;
                        }
                    } catch (Exception ignored) {}
                }

                sb.append(String.format(
                    "{\"id\":\"%s\",\"copyId\":\"%s\",\"userId\":\"%s\",\"userName\":\"%s\",\"userEmail\":\"%s\"," +
                    "\"bookId\":\"%s\",\"bookTitle\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\",\"returnDate\":\"%s\"," +
                    "\"status\":\"%s\",\"isOverdue\":%b,\"daysOverdue\":%d,\"fine\":%.2f}",
                    escape(transId), escape(copyId), escape(userId), escape(userName), escape(userEmail),
                    escape(bookId), escape(bookTitle), escape(issueDate), escape(dueDateStr), escape(returnDate != null ? returnDate : ""),
                    escape(status), isOverdue, daysOverdue, fine
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleIssue(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String userId = data.get("userId");
        String bookId = data.get("bookId");

        String result = circulationService.issueBook(userId, bookId);

        if (result.startsWith("Success")) {
            sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"" + escape(result) + "\"}");
        } else {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"" + escape(result) + "\"}");
        }
    }

    private void handleReturn(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String copyId = data.get("copyId");

        String result = circulationService.returnBook(copyId);

        if (result.startsWith("Success")) {
            sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"" + escape(result) + "\"}");
        } else {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"" + escape(result) + "\"}");
        }
    }

    private void handleGetMembers(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        String sql = "SELECT u.id, u.name, u.email, u.phone, u.class_name, u.roll_number, u.password_hash, COALESCE(r.role_name, 'STUDENT') as role_name, " +
                     "COALESCE((SELECT COUNT(*) FROM transactions t WHERE t.user_id = u.id AND t.status = 'ACTIVE'), 0) as active_loans " +
                     "FROM users u " +
                     "LEFT JOIN roles r ON u.role_id = r.id " +
                     "ORDER BY (CASE WHEN u.id = 'U9CC6D' OR LOWER(u.email) = 'rahul@gmail.com' THEN 0 ELSE 1 END), u.created_at DESC, u.id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;

                String id = rs.getString("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String className = rs.getString("class_name");
                String rollNumber = rs.getString("roll_number");
                String password = rs.getString("password_hash");
                String role = rs.getString("role_name");
                int activeLoans = rs.getInt("active_loans");

                sb.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"className\":\"%s\",\"rollNumber\":\"%s\",\"password\":\"%s\",\"role\":\"%s\",\"activeLoans\":%d}",
                    escape(id), escape(name), escape(email),
                    escape(phone != null ? phone : ""),
                    escape(className != null ? className : ""),
                    escape(rollNumber != null ? rollNumber : ""),
                    escape(password != null ? password : ""),
                    escape(role), activeLoans
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    // Utilities
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        String content = json.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
        }
        String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim();
                if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                    v = v.substring(1, v.length() - 1);
                }
                map.put(k, v);
            }
        }
        return map;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
