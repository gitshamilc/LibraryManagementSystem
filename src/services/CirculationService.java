package services;

import dao.BookDAO;
import dao.TransactionDAO;
import dao.UserDAO;
import models.BookCopy;
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

    public String issueBook(String userId, String bookId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            
            // 1. Check if user exists and is eligible
            PreparedStatement userPs = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            userPs.setString(1, userId);
            if (!userPs.executeQuery().next()) {
                return "Error: User not found.";
            }

            // 2. Find an available physical copy of the book
            PreparedStatement copyPs = conn.prepareStatement("SELECT id FROM book_copies WHERE book_id = ? AND status = 'AVAILABLE' LIMIT 1");
            copyPs.setString(1, bookId);
            ResultSet copyRs = copyPs.executeQuery();
            
            if (!copyRs.next()) {
                return "Error: No available physical copies for this book.";
            }
            String copyId = copyRs.getString("id");

            // 3. Create Transaction
            String transactionId = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String issueDate = LocalDate.now().toString();
            String dueDate = LocalDate.now().plusDays(14).toString(); // 14 days loan period

            Transaction transaction = new Transaction(transactionId, copyId, userId, issueDate, dueDate, null, "ACTIVE");
            transactionDAO.save(transaction);

            // 4. Update Copy Status
            PreparedStatement updateCopy = conn.prepareStatement("UPDATE book_copies SET status = 'ISSUED' WHERE id = ?");
            updateCopy.setString(1, copyId);
            updateCopy.executeUpdate();

            return "Success: Book issued successfully. Due date: " + dueDate;

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure during issue process.";
        }
    }

    public String returnBook(String copyId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            
            // 1. Find active transaction for this copy
            PreparedStatement transPs = conn.prepareStatement("SELECT * FROM transactions WHERE copy_id = ? AND status = 'ACTIVE'");
            transPs.setString(1, copyId);
            ResultSet transRs = transPs.executeQuery();
            
            if (!transRs.next()) {
                return "Error: No active loan found for this physical copy.";
            }
            
            String transactionId = transRs.getString("id");
            LocalDate dueDate = LocalDate.parse(transRs.getString("due_date"));
            LocalDate returnDate = LocalDate.now();

            // 2. Calculate Fine if overdue (₹5 per day)
            double fineAmount = 0.0;
            if (returnDate.isAfter(dueDate)) {
                long daysOverdue = ChronoUnit.DAYS.between(dueDate, returnDate);
                fineAmount = daysOverdue * 5.0; // configurable fine amount
                
                PreparedStatement finePs = conn.prepareStatement("INSERT INTO fines (transaction_id, amount, reason, status) VALUES (?, ?, ?, 'UNPAID')");
                finePs.setString(1, transactionId);
                finePs.setDouble(2, fineAmount);
                finePs.setString(3, "Overdue by " + daysOverdue + " days");
                finePs.executeUpdate();
            }

            // 3. Update Transaction
            PreparedStatement updateTrans = conn.prepareStatement("UPDATE transactions SET status = 'RETURNED', return_date = ? WHERE id = ?");
            updateTrans.setString(1, returnDate.toString());
            updateTrans.setString(2, transactionId);
            updateTrans.executeUpdate();

            // 4. Update Copy Status
            PreparedStatement updateCopy = conn.prepareStatement("UPDATE book_copies SET status = 'AVAILABLE' WHERE id = ?");
            updateCopy.setString(1, copyId);
            updateCopy.executeUpdate();

            if (fineAmount > 0) {
                return "Success: Book returned. Note: Late fine of ₹" + fineAmount + " applied.";
            } else {
                return "Success: Book returned on time.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database failure during return process.";
        }
    }
}
