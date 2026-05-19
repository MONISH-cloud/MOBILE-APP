package com.pgmanager.app.data.model;

import com.google.firebase.Timestamp;

public class Expense {
    private String id;
    private String pgId;
    private String category;
    private double amount;
    private String description;
    private Timestamp date;
    private Timestamp createdAt;

    public Expense() {} // Required for Firestore

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPgId() { return pgId; }
    public void setPgId(String pgId) { this.pgId = pgId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
