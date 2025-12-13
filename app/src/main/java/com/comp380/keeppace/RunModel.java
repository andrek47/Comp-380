package com.comp380.keeppace;

import com.google.firebase.Timestamp;

public class RunModel {
    private double distanceMeters;
    private long timeMillis;
    private int points;
    private Timestamp timestamp;

    public RunModel() {
        // Empty constructor needed for Firestore
    }

    public RunModel(double distanceMeters, long timeMillis, int points, Timestamp timestamp) {
        this.distanceMeters = distanceMeters;
        this.timeMillis = timeMillis;
        this.points = points;
        this.timestamp = timestamp;
    }

    // --- GETTERS ---
    public double getDistanceMeters() { return distanceMeters; }
    public long getTimeMillis() { return timeMillis; }
    public int getPoints() { return points; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
    public void setTimeMillis(long timeMillis) { this.timeMillis = timeMillis; }
    public void setPoints(int points) { this.points = points; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}