package com.example.demo_app.login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
public class LoginController {

    @Autowired private AuthenticationManager authManager;
    @Autowired private CustomUserDetailsService userDetailsService;

    // ==================== REGISTRATION ====================

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        var user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // ==================== LOGIN WITH JWT ====================

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = null;

        if (request.getToken() != null && !request.getToken().isEmpty()) {
            // Re-login with existing token
            var userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            authentication = authManager.authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userDetails,
                    request.getPassword(),
                    userDetails.getAuthorities()));
        } else if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // Initial login with username/password
            authentication = authManager.authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    new UserDetails() {
                        @Override public String getUsername() { return request.getUsername(); }
                        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return null; }
                    },
                    request.getPassword()));
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build(); // Unauthenticated
        }

        var authenticatedUser = userDetailsService.loadUserByUsername(request.getUsername());
        String token = JwtUtil.generateToken(authenticatedUser.getUsername(), authenticatedUser.getAuthorities());
        long expirationTime = System.currentTimeMillis() + 36_000_000L; // 1 hour in ms

        return ResponseEntity.ok(new LoginResponse(token, expirationTime));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<String> refreshToken(@RequestBody RefreshRequest request) {
        var token = JwtUtil.generateToken(request.getUsername(), null);
        long expirationTime = System.currentTimeMillis() + 36_000_000L;
        return ResponseEntity.ok(new LoginResponse(token, expirationTime));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@RequestBody LogoutRequest request) {
        // For token-based auth, "logout" means the client stops sending tokens.
        // You can invalidate the token by adding it to a blacklist if needed.
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/auth/me")
    public ResponseEntity<UserInfo> getCurrentUser() {
        var user = userRepository.findByUsername(null); // dummy, needs auth context
        return ResponseEntity.ok(user);
    }
}

