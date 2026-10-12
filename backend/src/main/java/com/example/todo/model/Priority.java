package com.example.todo.model;

/**
 * Stored as ORDINAL so that sorting by priority works naturally (LOW < MEDIUM < HIGH).
 * Only append new values at the end.
 */
public enum Priority {
    LOW, MEDIUM, HIGH
}
