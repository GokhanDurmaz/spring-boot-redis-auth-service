package com.example.demo_app.login;

import java.util.Date;
import java.util.Map;

public class LoginResponse {
    private String token;
    private Date expirationDate;
    private long expirationTime;
    private Map<String, Object> extraData;

    public LoginResponse(String token) {}
}

