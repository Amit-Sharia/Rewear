package com.rewear.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IDataAccessObject<T, ID> {

    
    ID insert(T entity) throws SQLException;


    Optional<T> findById(ID id) throws SQLException;

    
    List<T> findAll() throws SQLException;

    
    void update(T entity) throws SQLException;

    
    void delete(ID id) throws SQLException;
}
