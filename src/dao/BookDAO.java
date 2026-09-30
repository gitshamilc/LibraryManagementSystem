package dao;

import models.Book;
import core.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDAO implements GenericDAO<Book, String> {

    @Override
    public Book findById(String id) {
        String query = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Book(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("isbn"),
                    rs.getInt("author_id"),
                    rs.getInt("category_id"),
                    rs.getInt("publisher_id"),
                    rs.getString("cover_image_url"),
                    rs.getString("description")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("isbn"),
                    rs.getInt("author_id"),
                    rs.getInt("category_id"),
                    rs.getInt("publisher_id"),
                    rs.getString("cover_image_url"),
                    rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    @Override
    public boolean save(Book entity) {
        String query = "INSERT INTO books (id, title, isbn, author_id, category_id, publisher_id, cover_image_url, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, entity.getId());
            ps.setString(2, entity.getTitle());
            ps.setString(3, entity.getIsbn());
            ps.setInt(4, entity.getAuthorId());
            ps.setInt(5, entity.getCategoryId());
            ps.setInt(6, entity.getPublisherId());
            ps.setString(7, entity.getCoverImageUrl());
            ps.setString(8, entity.getDescription());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Book entity) {
        String query = "UPDATE books SET title = ?, isbn = ?, author_id = ?, category_id = ?, publisher_id = ?, cover_image_url = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, entity.getTitle());
            ps.setString(2, entity.getIsbn());
            ps.setInt(3, entity.getAuthorId());
            ps.setInt(4, entity.getCategoryId());
            ps.setInt(5, entity.getPublisherId());
            ps.setString(6, entity.getCoverImageUrl());
            ps.setString(7, entity.getDescription());
            ps.setString(8, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        String query = "DELETE FROM books WHERE id = ?";
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
