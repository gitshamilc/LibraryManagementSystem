package dao;

import models.Transaction;
import core.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO implements GenericDAO<Transaction, String> {

    @Override
    public Transaction findById(String id) {
        String query = "SELECT * FROM transactions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Transaction(
                    rs.getString("id"),
                    rs.getString("copy_id"),
                    rs.getString("user_id"),
                    rs.getString("issue_date"),
                    rs.getString("due_date"),
                    rs.getString("return_date"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM transactions";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                transactions.add(new Transaction(
                    rs.getString("id"),
                    rs.getString("copy_id"),
                    rs.getString("user_id"),
                    rs.getString("issue_date"),
                    rs.getString("due_date"),
                    rs.getString("return_date"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    @Override
    public boolean save(Transaction entity) {
        String query = "INSERT INTO transactions (id, copy_id, user_id, issue_date, due_date, return_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, entity.getId());
            ps.setString(2, entity.getCopyId());
            ps.setString(3, entity.getUserId());
            ps.setString(4, entity.getIssueDate());
            ps.setString(5, entity.getDueDate());
            ps.setString(6, entity.getReturnDate());
            ps.setString(7, entity.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Transaction entity) {
        String query = "UPDATE transactions SET copy_id = ?, user_id = ?, issue_date = ?, due_date = ?, return_date = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, entity.getCopyId());
            ps.setString(2, entity.getUserId());
            ps.setString(3, entity.getIssueDate());
            ps.setString(4, entity.getDueDate());
            ps.setString(5, entity.getReturnDate());
            ps.setString(6, entity.getStatus());
            ps.setString(7, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        String query = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
