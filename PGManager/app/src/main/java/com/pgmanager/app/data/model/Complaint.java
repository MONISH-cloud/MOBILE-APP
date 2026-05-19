package com.pgmanager.app.data.model;

import com.google.firebase.Timestamp;

public class Complaint {
    private String id;
    private String pgId;
    private String tenantId;
    private String tenantName;
    private String unit;
    private String description;
    private String priority; // "urgent" | "normal" | "low"
    private String status;   // "pending" | "in_progress" | "resolved"
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Complaint() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPgId() { return pgId; }
    public void setPgId(String pgId) { this.pgId = pgId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
