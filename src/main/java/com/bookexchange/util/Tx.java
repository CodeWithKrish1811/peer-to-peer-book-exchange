package com.bookexchange.util;

import com.bookexchange.config.DBConnection;
import com.bookexchange.exception.AppException;
import com.bookexchange.exception.DatabaseException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Small helper that owns the JDBC connection life-cycle for the service layer.
 *
 *  - read():  autocommit connection for plain SELECTs.
 *  - write(): one TRANSACTION. setAutoCommit(false), commit() on success and
 *             rollback() if anything (business rule, SQL error, bug) is thrown.
 * Connections are always closed by try-with-resources.
 */
public final class Tx {
    private Tx() { }

    /** A unit of database work that may throw SQL or business exceptions. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException, AppException;
    }

    public static <T> T read(Work<T> work) throws AppException {
        try (Connection c = DBConnection.getConnection()) {
            return work.run(c);
        } catch (SQLException e) {
            throw new DatabaseException("Could not reach the database. Please try again shortly.", e);
        }
    }

    public static <T> T write(Work<T> work) throws AppException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | AppException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save your changes. Please try again.", e);
        }
    }
}
