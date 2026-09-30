package dao;

import java.util.List;

public interface GenericDAO<T, K> {
    T findById(K id);
    List<T> findAll();
    boolean save(T entity);
    boolean update(T entity);
    boolean delete(K id);
}
