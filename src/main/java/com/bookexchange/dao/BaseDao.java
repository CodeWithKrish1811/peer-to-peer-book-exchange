package com.bookexchange.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Contract shared by the DAOs (ABSTRACTION / interface).
 * Every method receives the Connection so that a service can group several DAO
 * calls into ONE transaction.
 */
public interface BaseDao<T> {
    Optional<T> findById(Connection connection, int id) throws SQLException;
}
