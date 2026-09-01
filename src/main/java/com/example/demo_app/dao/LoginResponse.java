package com.example.demo_app.dao;

public class LoginResponse {
    private String token;
    private long expirationTime;

    public LoginResponse(String token, long expirationTime) {
        this.token = token;
        this.expirationTime = expirationTime;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpirationTime() { return expirationTime; }
    public void setExpirationTime(long expirationTime) { this.expirationTime = expirationTime; }
}
