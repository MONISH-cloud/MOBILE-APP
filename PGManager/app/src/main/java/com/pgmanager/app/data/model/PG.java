package com.pgmanager.app.data.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class PG {
    private String id;
    private String ownerId;
    private List<String> managerIds;
    private String name;
    private String address;
    private double defaultRent;
    private double defaultAdvance;
    private int totalRooms;
    private int totalBeds;
    private int occupiedBeds;
    private String photoUrl;
    private double latitude;
    private double longitude;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public PG() {} // Required for Firestore

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public List<String> getManagerIds() { return managerIds; }
    public void setManagerIds(List<String> managerIds) { this.managerIds = managerIds; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getDefaultRent() { return defaultRent; }
    public void setDefaultRent(double defaultRent) { this.defaultRent = defaultRent; }
    public double getDefaultAdvance() { return defaultAdvance; }
    public void setDefaultAdvance(double defaultAdvance) { this.defaultAdvance = defaultAdvance; }
    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }
    public int getTotalBeds() { return totalBeds; }
    public void setTotalBeds(int totalBeds) { this.totalBeds = totalBeds; }
    public int getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(int occupiedBeds) { this.occupiedBeds = occupiedBeds; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
