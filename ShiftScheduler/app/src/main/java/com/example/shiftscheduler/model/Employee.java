package com.example.shiftscheduler.model;

import com.example.shiftscheduler.adapters.EmployeeAdapter;

public class Employee {
    private String uid;
    private String name;
    private String teamId;

    // New fields for employer user manager
    private String email;
    private String role;
    private String employerId;
    private boolean active;

    public Employee() {
        // Needed for Firebase
    }

    // Existing constructor – DO NOT REMOVE (other code may use this)
    public Employee(String uid, String name, String teamId) {
        this.uid = uid;
        this.name = name;
        this.teamId = teamId;
    }

    // New "full" constructor (optional, only if you want it)
    public Employee(String uid,
                    String name,
                    String teamId,
                    String email,
                    String role,
                    String employerId,
                    boolean active) {
        this.uid = uid;
        this.name = name;
        this.teamId = teamId;
        this.email = email;
        this.role = role;
        this.employerId = employerId;
        this.active = active;
    }

    // ===== Getters =====

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        // default to "employee" if null
        return role != null ? role : "employee";
    }

    public String getEmployerId() {
        return employerId;
    }

    public boolean isActive() {
        return active;
    }

    // ===== Setters =====

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Keep old method name so existing code still compiles
    public void setteamId(String teamId) {
        this.teamId = teamId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setEmployerId(String employerId) {
        this.employerId = employerId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
