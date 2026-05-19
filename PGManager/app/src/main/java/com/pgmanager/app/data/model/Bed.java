package com.pgmanager.app.data.model;

public class Bed {
    private String id;
    private String bedNumber;
    private String status; // "vacant" | "occupied"
    private String tenantId;
    private String tenantName;

    public Bed() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
}
