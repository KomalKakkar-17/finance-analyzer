package com.financeanalyzer.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object (DAO) interface.
 * Demonstrates Unit 2 Generics and Unit 4 DAO pattern foundations.
 *
 * @param <T>  the entity type
 * @param <ID> the primary key identifier type
 */
public interface Dao<T, ID> {

    /**
     * Finds an entity by its primary key ID.
     *
     * @param id the entity identifier
     * @return an Optional containing the entity if found
     */
    Optional<T> findById(ID id);

    /**
     * Finds all entities scoped by the given user ID.
     *
     * @param userId the user ID owning the entities
     * @return list of entities
     */
    List<T> findAll(Long userId);

    /**
     * Persists a new entity.
     *
     * @param entity the entity to save
     * @return the saved entity with generated ID populated
     */
    T save(T entity);

    /**
     * Updates an existing entity.
     *
     * @param entity the entity to update
     * @return true if updated successfully, false otherwise
     */
    boolean update(T entity);

    /**
     * Deletes an entity by ID scoped to a specific user.
     *
     * @param id the entity ID
     * @param userId the owner user ID
     * @return true if deleted, false otherwise
     */
    boolean deleteById(ID id, Long userId);
}
