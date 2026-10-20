package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;

/**
 * Abstract parent for any person who uses the application.
 */
public abstract class User {

    private final String userId;
    private String name;
    private String email;

    protected User(String userId, String name, String email) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidInputException("User ID cannot be empty.");
        }
        this.userId = userId.trim();
        setName(name);
        setEmail(email);
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    /** Email is optional, but if given it must look like an email. */
    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            this.email = "";
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new InvalidInputException("Email address is not valid.");
        }
        this.email = email.trim();
    }

    /** Each subclass describes its own role. */
    public abstract String getRole();

    @Override
    public String toString() {
        return getRole() + ": " + name + " (" + userId + ")";
    }
}