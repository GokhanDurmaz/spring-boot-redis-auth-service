package com.example.demo_app.login;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.example.demo_app.dto.LoginResponse;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class CustomAuthenticationManager {
    private final AuthenticationManager authManager;

    public LoginResponse authenticate(String username, String password) {
        var token = new UsernamePasswordAuthenticationToken(username, password);
        var authenticatedUser = authManager.authenticate(token);
        return new LoginResponse(authenticatedUser.getName(), 3600_000L); // 1 hour in ms
    }
}
