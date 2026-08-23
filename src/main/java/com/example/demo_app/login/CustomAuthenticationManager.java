package com.example.demo_app.login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class CustomAuthenticationManager {
    private final AuthenticationManager authManager;

    @Autowired
    public CustomAuthenticationManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    public LoginResponse authenticate(String username, String password) {
        var token = new UsernamePasswordAuthenticationToken(username, password);
        var authenticatedUser = authManager.authenticate(token);
        return new LoginResponse(authenticatedUser.getName(), 3600_000L); // 1 hour in ms
    }
}
