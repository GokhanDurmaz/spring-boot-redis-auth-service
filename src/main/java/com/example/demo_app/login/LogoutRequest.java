package com.example.demo_app.login;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequest {
    @NotBlank(message = "Username is required")
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    // Default constructor for JSON deserialization
    private LogoutRequest() {}
}
