package com.example.demo_app.login;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
public class LoginController {

    @Autowired private AuthenticationManager authManager;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;

    // ==================== REGISTRATION ====================

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        var user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // ==================== LOGIN WITH JWT ====================

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = null;

        if (request.getToken() != null && !request.getToken().isEmpty()) {
            // Re-login with existing token - verify the token first
            var userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            if (userDetails == null) {
                return ResponseEntity.status(401).build();
            }
            authentication = authManager.authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userDetails,
                    request.getPassword(),
                    userDetails.getAuthorities()
            ));
        } else if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // Initial login with username/password
            var user = userRepository.findByUsername(request.getUsername());
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(401).build();
            }
            authentication = authManager.authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userDetails,
                    request.getPassword()
            ));
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(null);
        }

        var authenticatedUser = userDetailsService.loadUserByUsername(request.getUsername());
        String token = JwtUtil.generateToken(authenticatedUser.getUsername(), 
                authenticatedUser.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(java.util.stream.Collectors.toList()));
        long expirationTime = System.currentTimeMillis() + 36_000_000L; // 1 hour in ms

        return ResponseEntity.ok(new LoginResponse(token, expirationTime));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        var token = JwtUtil.generateToken(request.getUsername(), null);
        long expirationTime = System.currentTimeMillis() + 36_000_000L;
        return ResponseEntity.ok(new LoginResponse(token, expirationTime));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody LogoutRequest request) {
        // For token-based auth, "logout" means the client stops sending tokens.
        // You can invalidate the token by adding it to a blacklist if needed.
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/auth/me")
    public ResponseEntity<UserInfo> getCurrentUser() {
        var user = userRepository.findByUsername(null); // dummy, needs auth context
        return ResponseEntity.ok(new UserInfo(user.getUsername(), "user@example.com"));
    }
}
