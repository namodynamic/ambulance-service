package com.ambulance.ambulance_service.dto;

/**
 * DTO class for admin dashboard statistics
 */
public class AdminDashboardStats {
    // Request stats
    private long totalRequests;
    private long pendingRequests;
    private long completedRequests;
    private long inProgressRequests;

    // Ambulance stats
    private long totalAmbulances;
    private long availableAmbulances;
    private long dispatchedAmbulances;
    private long onDutyAmbulances;

    // Patient stats
    private long totalPatients;

    // Getters and Setters
    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

    public long getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(long pendingRequests) { this.pendingRequests = pendingRequests; }

    public long getCompletedRequests() { return completedRequests; }
    public void setCompletedRequests(long completedRequests) { this.completedRequests = completedRequests; }

    public long getInProgressRequests() { return inProgressRequests; }
    public void setInProgressRequests(long inProgressRequests) { this.inProgressRequests = inProgressRequests; }

    public long getTotalAmbulances() { return totalAmbulances; }
    public void setTotalAmbulances(long totalAmbulances) { this.totalAmbulances = totalAmbulances; }

    public long getAvailableAmbulances() { return availableAmbulances; }
    public void setAvailableAmbulances(long availableAmbulances) { this.availableAmbulances = availableAmbulances; }

    public long getDispatchedAmbulances() { return dispatchedAmbulances; }
    public void setDispatchedAmbulances(long dispatchedAmbulances) { this.dispatchedAmbulances = dispatchedAmbulances; }

    public long getOnDutyAmbulances() { return onDutyAmbulances; }
    public void setOnDutyAmbulances(long onDutyAmbulances) { this.onDutyAmbulances = onDutyAmbulances; }

    public long getTotalPatients() { return totalPatients; }
    public void setTotalPatients(long totalPatients) { this.totalPatients = totalPatients; }
}
