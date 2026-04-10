package com.rewear.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object interface defining standard CRUD operations.
 * All DAO implementations must follow this contract.
 *
 * This interface enables POLYMORPHISM:
 * - UserDAO, ItemDAO, ChatDAO all implement the same interface
 * - Same method names (insert, findById, etc.) but different behaviors
 * - Allows writing generic code that works with any entity type
 *
 * Example polymorphism:
 *   IDataAccessObject<User, Integer> userDao = new UserDAO();
 *   IDataAccessObject<Item, Integer> itemDao = new ItemDAO();
 *   // Same interface, different implementations!
 *
 * @param <T> the entity type managed by this DAO
 * @param <ID> the primary key type
 */
public interface IDataAccessObject<T, ID> {

    /**
     * Insert a new entity into the database.
     *
     * @param entity the entity to insert
     * @return the generated ID of the inserted entity
     * @throws SQLException if database operation fails
     */
    ID insert(T entity) throws SQLException;

    /**
     * Find an entity by its primary key.
     *
     * @param id the primary key
     * @return an Optional containing the entity if found, empty otherwise
     * @throws SQLException if database operation fails
     */
    Optional<T> findById(ID id) throws SQLException;

    /**
     * Find all entities of this type.
     *
     * @return a list of all entities
     * @throws SQLException if database operation fails
     */
    List<T> findAll() throws SQLException;

    /**
     * Update an existing entity.
     *
     * @param entity the entity to update
     * @throws SQLException if database operation fails
     */
    void update(T entity) throws SQLException;

    /**
     * Delete an entity by its primary key.
     *
     * @param id the primary key
     * @throws SQLException if database operation fails
     */
    void delete(ID id) throws SQLException;
}
