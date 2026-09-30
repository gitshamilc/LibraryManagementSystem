import com.sun.net.httpserver.HttpServer;
import core.DatabaseManager;
import core.DatabaseSeeder;
import controllers.StaticFileHandler;
import controllers.ApiController;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing Database...");
        DatabaseManager.initializeDatabase();
        DatabaseSeeder.seed();

        int port = 8080;
        HttpServer server = null;
        
        while (port < 8100) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (IOException e) {
                port++;
            }
        }

        if (server == null) {
            System.err.println("Failed to start server: Could not bind to any port between 8080 and 8100.");
            return;
        }

        // Serve static files from web directory
        server.createContext("/", new StaticFileHandler("web"));
        
        // REST API endpoints
        ApiController apiController = new ApiController();
        server.createContext("/api/login", apiController);
        server.createContext("/api/stats", apiController);
        server.createContext("/api/books", apiController);
        
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        String url = "http://localhost:" + port;
        System.out.println("\n=======================================================");
        System.out.println("  GP6 LIBRARY SYSTEM (MASTER UPGRADE) IS RUNNING!");
        System.out.println("  Web Interface: " + url);
        System.out.println("  Admin Login  : admin@example.com / Shamil@123");
        System.out.println("=======================================================\n");

        openBrowser(url);
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    Runtime.getRuntime().exec("xdg-open " + url);
                }
            }
        } catch (Exception ignored) {}
    }
}
