package com.example.demo_app.controller;

import com.example.demo_app.dao.LoginRequest;
import com.example.demo_app.dao.RegisterRequest;
import com.example.demo_app.entity.User;
import com.example.demo_app.login.JwtUtil;
import com.example.demo_app.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginController(AuthenticationManager authenticationManager, 
                           UserRepository userRepository, 
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String token = JwtUtil.generateToken(authentication.getName(), authentication.getAuthorities());

            Map<String, String> response = new HashMap<>();
            response.put("token", token);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect username or password!"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username is already taken!"));
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));

        userRepository.save(user);

        // Otomatik login için token üretimi
        String token = JwtUtil.generateToken(user.getUsername(), user.getAuthorities());

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("message", "User registered successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
