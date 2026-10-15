package services;

import dao.TransactionDAO;
import models.Transaction;
import core.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class CirculationService {
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public String issueBook(String userIdentifier, String bookIdentifier) {
        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            return "Error: Member ID or Email is required.";
        }
        if (bookIdentifier == null || bookIdentifier.trim().isEmpty()) {
            return "Error: Book ID or Copy Barcode is required.";
        }

        userIdentifier = userIdentifier.trim();
        bookIdentifier = bookIdentifier.trim();

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Check user existence and active status
            String userId = null;
            String userName = null;
            PreparedStatement userPs = conn.prepareStatement("SELECT id, name FROM users WHERE id = ? OR LOWER(email) = ? OR LOWER(name) = ?");
            userPs.setString(1, userIdentifier);
            userPs.setString(2, userIdentifier.toLowerCase());
            userPs.setString(3, userIdentifier.toLowerCase());
            ResultSet userRs = userPs.executeQuery();
            if (userRs.next()) {
                userId = userRs.getString("id");
                userName = userRs.getString("name");
            } else {
                return "Error: Member '" + userIdentifier + "' not found in system.";
            }

            // 2. Check active loans count (max 5 books per user limit)
            PreparedStatement loanCountPs = conn.prepareStatement("SELECT COUNT(*) FROM transactions WHERE user_id = ? AND status = 'ACTIVE'");
            loanCountPs.setString(1, userId);
            ResultSet loanCountRs = loanCountPs.executeQuery();
            if (loanCountRs.next() && loanCountRs.getInt(1) >= 5) {
                return "Error: Member has reached maximum borrowing limit (5 active loans). Please return a book first.";
            }

            // 3. Find an available physical copy
            String copyId = null;
            String bookTitle = "Book";

            // Check if input is a direct copy ID or barcode
            PreparedStatement directCopyPs = conn.prepareStatement(
                "SELECT c.id, b.title FROM book_copies c JOIN books b ON c.book_id = b.id WHERE (c.id = ? OR c.barcode = ?) AND c.status = 'AVAILABLE' LIMIT 1"
            );
            directCopyPs.setString(1, bookIdentifier);
            directCopyPs.setString(2, bookIdentifier);
            ResultSet directCopyRs = directCopyPs.executeQuery();
            if (directCopyRs.next()) {
                copyId = directCopyRs.getString("id");
                bookTitle = directCopyRs.getString("title");
            } else {
                // Check by book ID or ISBN
                PreparedStatement bookPs = conn.prepareStatement(
                    "SELECT c.id, b.title FROM books b LEFT JOIN book_copies c ON b.id = c.book_id AND c.status = 'AVAILABLE' WHERE b.id = ? OR b.isbn = ? LIMIT 1"
                );
                bookPs.setString(1, bookIdentifier);
                bookPs.setString(2, bookIdentifier);
                ResultSet bookRs = bookPs.executeQuery();
                if (bookRs.next()) {
                    copyId = bookRs.getString("id");
                    bookTitle = bookRs.getString("title");
                    if (copyId == null) {
                        return "Error: All physical copies of '" + bookTitle + "' are currently checked out.";
                    }
                } else {
                    return "Error: Book '" + bookIdentifier + "' not found in catalog.";
                }
            }

            // 4. Create Transaction
            String transactionId = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String issueDate = LocalDate.now().toString();
            String dueDate = LocalDate.now().plusDays(14).toString();

            Transaction transaction = new Transaction(transactionId, copyId, userId, issueDate, dueDate, null, "ACTIVE");
            transactionDAO.save(transaction);

            // 5. Update Copy Status
            PreparedStatement updateCopy = conn.prepareStatement("UPDATE book_copies SET status = 'ISSUED' WHERE id = ?");
            updateCopy.setString(1, copyId);
            updateCopy.executeUpdate();

            // 6. Audit Log
            try {
                PreparedStatement audit = conn.prepareStatement("INSERT INTO audit_logs (user_id, action, details) VALUES (?, 'ISSUE', ?)");
                audit.setString(1, userId);
                audit.setString(2, "Issued copy " + copyId + " of '" + bookTitle + "' to " + userName);
                audit.executeUpdate();
            } catch (Exception ignored) {}

            return "Success: '" + bookTitle + "' (Copy " + copyId + ") issued to " + userName + ". Due: " + dueDate;

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure during issue process: " + e.getMessage();
        }
    }

    public String returnBook(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return "Error: Copy ID, Barcode, or Book ID is required.";
        }
        identifier = identifier.trim();

        try (Connection conn = DatabaseManager.getConnection()) {
            // Find active transaction by copy ID, barcode, book ID, or transaction ID
            PreparedStatement transPs = conn.prepareStatement(
                "SELECT t.id, t.copy_id, t.user_id, t.due_date, b.title as book_title, u.name as user_name " +
                "FROM transactions t " +
                "JOIN book_copies c ON t.copy_id = c.id " +
                "JOIN books b ON c.book_id = b.id " +
                "JOIN users u ON t.user_id = u.id " +
                "WHERE (c.id = ? OR c.barcode = ? OR c.book_id = ? OR t.id = ?) AND t.status = 'ACTIVE' " +
                "LIMIT 1"
            );
            transPs.setString(1, identifier);
            transPs.setString(2, identifier);
            transPs.setString(3, identifier);
            transPs.setString(4, identifier);
            ResultSet transRs = transPs.executeQuery();

            if (!transRs.next()) {
                return "Error: No active loan found for '" + identifier + "'. Book is already available or not registered.";
            }

            String transactionId = transRs.getString("id");
            String copyId = transRs.getString("copy_id");
            String userId = transRs.getString("user_id");
            String bookTitle = transRs.getString("book_title");
            String userName = transRs.getString("user_name");
            String dueDateStr = transRs.getString("due_date");
            LocalDate dueDate = LocalDate.parse(dueDateStr.length() > 10 ? dueDateStr.substring(0, 10) : dueDateStr);
            LocalDate returnDate = LocalDate.now();

            // Calculate Fine (₹5 per day late)
            double fineAmount = 0.0;
            long daysOverdue = 0;
            if (returnDate.isAfter(dueDate)) {
                daysOverdue = ChronoUnit.DAYS.between(dueDate, returnDate);
                fineAmount = daysOverdue * 5.0;

                PreparedStatement finePs = conn.prepareStatement(
                    "INSERT INTO fines (transaction_id, amount, reason, status) VALUES (?, ?, ?, 'UNPAID')"
                );
                finePs.setString(1, transactionId);
                finePs.setDouble(2, fineAmount);
                finePs.setString(3, "Overdue by " + daysOverdue + " days");
                finePs.executeUpdate();
            }

            // Update Transaction
            PreparedStatement updateTrans = conn.prepareStatement(
                "UPDATE transactions SET status = 'RETURNED', return_date = ? WHERE id = ?"
            );
            updateTrans.setString(1, returnDate.toString());
            updateTrans.setString(2, transactionId);
            updateTrans.executeUpdate();

            // Update Copy Status
            PreparedStatement updateCopy = conn.prepareStatement(
                "UPDATE book_copies SET status = 'AVAILABLE' WHERE id = ?"
            );
            updateCopy.setString(1, copyId);
            updateCopy.executeUpdate();

            // Audit log
            try {
                PreparedStatement audit = conn.prepareStatement("INSERT INTO audit_logs (user_id, action, details) VALUES (?, 'RETURN', ?)");
                audit.setString(1, userId);
                audit.setString(2, "Returned copy " + copyId + " of '" + bookTitle + "' by " + userName);
                audit.executeUpdate();
            } catch (Exception ignored) {}

            if (fineAmount > 0) {
                return "Success: '" + bookTitle + "' returned by " + userName + ". Late fine of ₹" + (int)fineAmount + " applied (" + daysOverdue + " days overdue).";
            } else {
                return "Success: '" + bookTitle + "' returned on time by " + userName + ".";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure during return process: " + e.getMessage();
        }
    }
}
