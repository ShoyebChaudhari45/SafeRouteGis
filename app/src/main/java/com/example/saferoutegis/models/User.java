package com.example.saferoutegis.models;

/**
 * Model class representing a registered user of the app.
 * Stored in the SQLite 'users' table.
 */
public class User {

    private int id;
    private String name;
    private String email;
    private String password; // SHA-256 hashed
    private String phone;
    private String createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    public User(String name, String email, String password, String phone, String createdAt) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
