package com.example.shiftscheduler.model;

import com.google.firebase.firestore.PropertyName;

public class ShiftRequest {

    private String requestId;   // Firestore document ID

    // ========== COMMON FIELDS ==========
    @PropertyName("employeeId")
    private String employeeId;

    @PropertyName("status")
    private String status;

    // ========== DATE FIELD (shared by both collections) ==========
    @PropertyName("date")
    private String shiftDate;

    // ========== SHIFTCHANGEREQUESTS-ONLY FIELDS ==========
    @PropertyName("shiftId")
    private String shiftId;

    @PropertyName("startTime")
    private String startTime;

    @PropertyName("endTime")
    private String endTime;

    // type = "calloff" only exists in shiftChangeRequests
    @PropertyName("type")
    private String type;

    // ========== TIMEOFFREQUESTS-ONLY FIELDS ==========
    @PropertyName("reason")
    private String reason;

    // empty constructor required for Firestore
    public ShiftRequest() {}

    // ====== Getters / Setters ======

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(String shiftDate) {
        this.shiftDate = shiftDate;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // Status (pending / approved / denied)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // REQUEST TYPE (calloff or time off)
    public String getType() {
        // TIME-OFF REQUESTS DO NOT HAVE A "type" FIELD
        if (type == null && reason != null) {
            return "time off";
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
