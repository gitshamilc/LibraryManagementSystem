package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private final String baseDir;

    public StaticFileHandler(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        File file = new File(baseDir + path).getCanonicalFile();
        
        // Prevent directory traversal attacks
        if (!file.getPath().startsWith(new File(baseDir).getCanonicalPath())) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        if (!file.exists() || file.isDirectory()) {
            String error = "JAVA_404_ERROR: Could not find " + file.getAbsolutePath();
            byte[] errorBytes = error.getBytes();
            exchange.sendResponseHeaders(404, errorBytes.length);
            exchange.getResponseBody().write(errorBytes);
            exchange.getResponseBody().close();
            return;
        }

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            if (path.endsWith(".css")) mimeType = "text/css";
            else if (path.endsWith(".js")) mimeType = "application/javascript";
            else mimeType = "application/octet-stream";
        }

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, file.length());

        try (OutputStream os = exchange.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = fs.read(buffer)) >= 0) {
                os.write(buffer, 0, count);
            }
        }
    }
}
