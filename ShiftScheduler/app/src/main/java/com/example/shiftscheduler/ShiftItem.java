package com.example.shiftscheduler;

import androidx.annotation.Keep;

/**
 * Model class representing a single shift or schedule-related item.
 * Used for:
 *  - Actual scheduled shifts
 *  - Shift change requests (trade / calloff)
 *  - Time-off requests (date-only blocks)
 */
@Keep
public class ShiftItem {

    // Firestore document id for /shifts
    private String shiftId;

    // Firestore document id for /shiftChangeRequests or /timeOffRequests
    private String requestId;

    // "trade", "calloff", "timeoff", or null when just a shift
    private String type;

    // Core shift data
    private String date;        // "yyyy-MM-dd"
    private String startTime;   // "HH:mm" or "h:mm a"
    private String endTime;
    private String role;        // position / job title

    // Employee data
    private String employeeId;   // UID
    private String employeeName; // resolved from employeeId

    // Status ("pending", "approved", "denied", "available_for_trade", etc.)
    private String status;

    // ===== REQUIRED: Empty constructor for Firebase =====
    public ShiftItem() {
    }

    // Convenience constructor used in EmployeeHomeActivity dummy data
    public ShiftItem(String date,
                     String startTime,
                     String endTime,
                     String role) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.role = role;
    }

    // Optional full constructor
    public ShiftItem(String shiftId,
                     String date,
                     String startTime,
                     String endTime,
                     String role,
                     String employeeId,
                     String employeeName,
                     String status,
                     String type,
                     String requestId) {
        this.shiftId = shiftId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.role = role;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.status = status;
        this.type = type;
        this.requestId = requestId;
    }

    // ===== Getters & Setters =====

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
