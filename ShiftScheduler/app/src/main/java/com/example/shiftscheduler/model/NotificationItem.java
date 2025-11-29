package com.example.shiftscheduler.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class NotificationItem {

    private String id; // Firestore document id

    @PropertyName("employeeId")
    private String employeeId;

    @PropertyName("title")
    private String title;

    @PropertyName("message")
    private String message;

    @PropertyName("type")
    private String type;          // e.g. "approval"

    @PropertyName("requestType")
    private String requestType;   // "time off" / "calloff"

    @PropertyName("requestId")
    private String requestId;

    @PropertyName("shiftId")
    private String shiftId;

    @PropertyName("createdAt")
    private Timestamp createdAt;

    @PropertyName("read")
    private boolean read;

    public NotificationItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getShiftId() { return shiftId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
