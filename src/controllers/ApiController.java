package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.BookDAO;
import dao.UserDAO;
import models.Book;
import models.User;
import services.CirculationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ApiController implements HttpHandler {
    private final BookDAO bookDAO = new BookDAO();
    private final UserDAO userDAO = new UserDAO();

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
            } else if (path.equals("/api/issue") && "POST".equalsIgnoreCase(method)) {
                handleIssue(exchange);
            } else if (path.equals("/api/return") && "POST".equalsIgnoreCase(method)) {
                handleReturn(exchange);
            } else {
                sendJsonResponse(exchange, 404, "{\"error\": \"Endpoint not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{\"error\": \"Internal server error\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String email = data.get("email");
        String password = data.get("password");

        User user = userDAO.authenticate(email, password);
        if (user != null) {
            String json = String.format("{\"success\": true, \"userId\": \"%s\", \"name\": \"%s\", \"roleId\": %d}", 
                user.getId(), user.getName(), user.getRoleId());
            sendJsonResponse(exchange, 200, json);
        } else {
            sendJsonResponse(exchange, 401, "{\"success\": false, \"error\": \"Invalid credentials\"}");
        }
    }

    private void handleSignup(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");

        if (name == null || email == null || password == null) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Missing required fields\"}");
            return;
        }

        UserDAO userDAO = new UserDAO();
        String newId = "U" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        User newUser = new User(newId, name, email, null, password, 3); // Role 3 = USER

        boolean success = userDAO.save(newUser);
        if (success) {
            sendJsonResponse(exchange, 200, "{\"success\": true}");
        } else {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Email may already exist.\"}");
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        // Mock stats for now, in a real app this comes from aggregate DB queries
        String json = "{\"totalBooks\": 100, \"activeMembers\": 30, \"activeLoans\": 45, \"overdueBooks\": 3, \"finesCollected\": 125.50}";
        sendJsonResponse(exchange, 200, json);
    }

    private void handleGetBooks(HttpExchange exchange) throws IOException {
        List<Book> books = bookDAO.findAll();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            sb.append(String.format("{\"id\":\"%s\",\"title\":\"%s\",\"authorId\":%d,\"categoryId\":%d,\"coverUrl\":\"%s\",\"description\":\"%s\"}",
                b.getId(), escape(b.getTitle()), b.getAuthorId(), b.getCategoryId(), b.getCoverImageUrl(), escape(b.getDescription())));
            if (i < books.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleIssue(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> data = parseJson(body);
        String userId = data.get("userId");
        String bookId = data.get("bookId");

        CirculationService service = new CirculationService();
        String result = service.issueBook(userId, bookId);
        
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

        CirculationService service = new CirculationService();
        String result = service.returnBook(copyId);
        
        if (result.startsWith("Success")) {
            sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"" + escape(result) + "\"}");
        } else {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"" + escape(result) + "\"}");
        }
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
        if (content.startsWith("{")) content = content.substring(1, content.length() - 1);
        String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replace("\"", "");
                String v = kv[1].trim();
                if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                map.put(k, v);
            }
        }
        return map;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
