package com.example.parkingspotsaver;

public class ParkingLocation {
    private double latitude;
    private double longitude;

    // No-argument constructor required for Firebase
    public ParkingLocation() {
    }

    public ParkingLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}