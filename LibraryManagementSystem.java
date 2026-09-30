import java.io.File;

public class LibraryManagementSystem {
    public static void main(String[] args) {
        System.out.println("===========================================================");
        System.out.println("  BOOTSTRAPPING GP6 LIBRARY SYSTEM (MASTER UPGRADE)");
        System.out.println("===========================================================");
        try {
            // Compile the new OOP multi-file system
            ProcessBuilder buildPb = new ProcessBuilder("cmd", "/c", "build.bat");
            buildPb.inheritIO();
            int buildResult = buildPb.start().waitFor();
            
            if (buildResult == 0) {
                // Run the new OOP multi-file system
                ProcessBuilder runPb = new ProcessBuilder("cmd", "/c", "run.bat");
                runPb.inheritIO();
                runPb.start().waitFor();
            } else {
                System.err.println("Failed to compile the new system.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
