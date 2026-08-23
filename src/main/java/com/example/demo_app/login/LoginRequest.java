package com.example.demo_app.login;

public class LoginRequest {
    private String username;
    private String password;   // kept for backward compatibility / registration
    private String token;      // JWT token to verify for re-login

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    // Default constructor for JSON deserialization
    private LoginRequest() {}
}

