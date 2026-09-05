package com.example.demo_app.dao;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequest {
    @NotBlank(message = "Username is required")
    private String username;

    public RefreshRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}