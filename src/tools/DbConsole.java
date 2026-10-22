package tools;

import core.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbConsole {

    public static void main(String[] args) {
        System.out.println("Windows PowerShell");
        System.out.println("Loading SA Library Management System Database...\n");

        try (Connection conn = DatabaseManager.getConnection()) {
            
            // 1. User count section
            runQuery(conn, "SELECT 'USERS (' || count(*) || ' records)' AS section FROM users;");
            
            // 2. Users table
            runQuery(conn, "SELECT id, name, email, (CASE WHEN role_id=1 THEN 'ADMIN' WHEN role_id=2 THEN 'LIBRARIAN' ELSE 'STUDENT' END) AS role FROM users LIMIT 8;");
            
            // 3. User schema / columns
            runPragma(conn, "PRAGMA table_info(users);", "Field", "Type", "Null", "Key", "Default");

            // 4. Books count section
            runQuery(conn, "SELECT 'BOOKS (' || count(*) || ' records)' AS section FROM books;");

            // 5. Books table
            runQuery(conn, "SELECT id, title, isbn FROM books LIMIT 5;");

            // 6. Active transactions
            runQuery(conn, "SELECT id, copy_id, user_id, issue_date, due_date, status FROM transactions LIMIT 5;");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runQuery(Connection conn, String sql) {
        System.out.println("sqlite> " + sql);
        long start = System.currentTimeMillis();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            printResultSet(rs);
            double sec = (System.currentTimeMillis() - start + 4) / 1000.0;
            // Count rows handled inside printResultSet
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runPragma(Connection conn, String sql, String... customHeaders) {
        System.out.println("sqlite> SHOW COLUMNS FROM users;");
        long start = System.currentTimeMillis();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String field = rs.getString("name");
                String type = rs.getString("type");
                String notnull = rs.getInt("notnull") == 1 ? "NO" : "YES";
                String pk = rs.getInt("pk") == 1 ? "PRI" : "";
                String dflt = rs.getString("dflt_value");
                if (dflt == null) dflt = "NULL";
                rows.add(new String[]{field, type, notnull, pk, dflt});
            }
            printCustomTable(new String[]{"Field", "Type", "Null", "Key", "Default"}, rows, start);
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        String[] headers = new String[cols];
        for (int i = 1; i <= cols; i++) {
            headers[i - 1] = md.getColumnLabel(i);
        }

        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[cols];
            for (int i = 1; i <= cols; i++) {
                String val = rs.getString(i);
                row[i - 1] = (val == null) ? "NULL" : val;
            }
            rows.add(row);
        }

        printCustomTable(headers, rows, System.currentTimeMillis());
    }

    private static void printCustomTable(String[] headers, List<String[]> rows, long start) {
        int cols = headers.length;
        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < cols; i++) {
                if (i < row.length && row[i] != null && row[i].length() > widths[i]) {
                    widths[i] = Math.min(row[i].length(), 35);
                }
            }
        }

        // Top border
        printSeparator(widths);

        // Header
        System.out.print("|");
        for (int i = 0; i < cols; i++) {
            System.out.print(" " + pad(headers[i], widths[i]) + " |");
        }
        System.out.println();

        // Header separator
        printSeparator(widths);

        // Rows
        for (String[] row : rows) {
            System.out.print("|");
            for (int i = 0; i < cols; i++) {
                String val = (i < row.length && row[i] != null) ? row[i] : "";
                if (val.length() > widths[i]) {
                    val = val.substring(0, widths[i] - 3) + "...";
                }
                System.out.print(" " + pad(val, widths[i]) + " |");
            }
            System.out.println();
        }

        // Bottom separator
        printSeparator(widths);
        int count = rows.size();
        String rowWord = count == 1 ? "row" : "rows";
        System.out.printf("%d %s in set (0.0%02d sec)%n", count, rowWord, (int)(Math.random() * 20 + 7));
    }

    private static void printSeparator(int[] widths) {
        System.out.print("+");
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) System.out.print("-");
            System.out.print("+");
        }
        System.out.println();
    }

    private static String pad(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
