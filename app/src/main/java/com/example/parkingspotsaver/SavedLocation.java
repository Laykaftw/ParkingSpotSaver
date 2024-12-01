package com.example.parkingspotsaver;

public class SavedLocation {
    private String id;
    private double latitude;
    private double longitude;
    private String note;

    // No-argument constructor required for Firebase
    public SavedLocation() {
    }

    public SavedLocation(String id, double latitude, double longitude, String note) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.note = note;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getNote() {
        return note;
    }
}