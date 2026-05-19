package com.pgmanager.app.data.model;

import com.google.firebase.Timestamp;

public class Tenant {
    private String id;
    private String pgId;
    private String roomId;
    private String bedId;
    private String pgName;
    private String roomNumber;
    private String bedNumber;
    private String name;
    private String whatsappNumber;
    private Timestamp joiningDate;
    private double rentAmount;
    private double advanceAmount;
    private String status;
    private Timestamp noticeDate;
    private Timestamp expectedMoveOutDate;
    private Timestamp createdAt;

    public Tenant() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPgId() { return pgId; }
    public void setPgId(String pgId) { this.pgId = pgId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getBedId() { return bedId; }
    public void setBedId(String bedId) { this.bedId = bedId; }
    public String getPgName() { return pgName; }
    public void setPgName(String pgName) { this.pgName = pgName; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public Timestamp getJoiningDate() { return joiningDate; }
    public void setJoiningDate(Timestamp joiningDate) { this.joiningDate = joiningDate; }
    public double getRentAmount() { return rentAmount; }
    public void setRentAmount(double rentAmount) { this.rentAmount = rentAmount; }
    public double getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(double advanceAmount) { this.advanceAmount = advanceAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getNoticeDate() { return noticeDate; }
    public void setNoticeDate(Timestamp noticeDate) { this.noticeDate = noticeDate; }
    public Timestamp getExpectedMoveOutDate() { return expectedMoveOutDate; }
    public void setExpectedMoveOutDate(Timestamp expectedMoveOutDate) { this.expectedMoveOutDate = expectedMoveOutDate; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
