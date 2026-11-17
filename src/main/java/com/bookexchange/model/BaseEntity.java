package com.bookexchange.model;

import java.sql.Timestamp;

/**
 * Abstract parent of every database-backed model class.
 * Demonstrates ABSTRACTION (abstract method), INHERITANCE and POLYMORPHISM
 * (each subclass overrides getSummary()).
 */
public abstract class BaseEntity {
    private Timestamp createdAt;

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /** A short human-readable description used in logs and messages. */
    public abstract String getSummary();

    @Override
    public String toString() { return getSummary(); }
}
