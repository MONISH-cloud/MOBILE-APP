package com.pgmanager.app.data.model;

import com.google.firebase.Timestamp;

public class UserProfile {
    private String userId;
    private String name;
    private String phone;
    private String role; // "owner" | "manager"
    private String subscriptionPlan; // "free" | "basic" | "pro"
    private int maxPGs;
    private Timestamp createdAt;

    public UserProfile() {} // Required for Firestore

    public UserProfile(String userId, String name, String phone, String role) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.subscriptionPlan = "free";
        this.maxPGs = 3;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }
    public int getMaxPGs() { return maxPGs; }
    public void setMaxPGs(int maxPGs) { this.maxPGs = maxPGs; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
