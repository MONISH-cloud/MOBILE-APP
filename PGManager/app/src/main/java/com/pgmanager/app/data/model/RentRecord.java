package com.pgmanager.app.data.model;

import com.google.firebase.Timestamp;

public class RentRecord {
    private String id;
    private String tenantId;
    private String pgId;
    private String tenantName;
    private String pgName;
    private String whatsappNumber;
    private String month; // e.g., "2026-04"
    private double amount;
    private Timestamp dueDate;
    private String status; // "pending" | "paid" | "overdue"
    private Timestamp paidDate;
    private double rewardApplied;
    private boolean reminderSent;
    private Timestamp createdAt;

    public RentRecord() {} // Required for Firestore

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getPgId() { return pgId; }
    public void setPgId(String pgId) { this.pgId = pgId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getPgName() { return pgName; }
    public void setPgName(String pgName) { this.pgName = pgName; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getPaidDate() { return paidDate; }
    public void setPaidDate(Timestamp paidDate) { this.paidDate = paidDate; }
    public double getRewardApplied() { return rewardApplied; }
    public void setRewardApplied(double rewardApplied) { this.rewardApplied = rewardApplied; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
