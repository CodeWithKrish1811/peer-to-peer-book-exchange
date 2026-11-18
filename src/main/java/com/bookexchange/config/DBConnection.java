package com.bookexchange.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Reusable JDBC connection factory.
 * Settings come from environment variables (DB_URL, DB_USER, DB_PASSWORD) first and
 * from src/main/resources/db.properties second, so no password has to be committed.
 */
public final class DBConnection {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() { }

    private static String setting(String envName, String propName, String fallback) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return fromEnv;
        }
        return PROPS.getProperty(propName, fallback);
    }

    /** Opens a NEW connection. The caller must close it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                setting("DB_URL", "db.url", "jdbc:mysql://localhost:3306/book_exchange_db"),
                setting("DB_USER", "db.user", "root"),
                setting("DB_PASSWORD", "db.password", ""));
    }
}
