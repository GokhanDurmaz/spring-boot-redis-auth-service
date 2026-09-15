package com.example.demo_app.controller;

import com.example.demo_app.dto.UserProfile;
import com.example.demo_app.entity.User;
import com.example.demo_app.service.CustomUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CustomUserDetailsService userDetailsService;

    public UserController(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userDetailsService.findUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfile profile = userDetailsService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfile profileDto) {
        
        UserProfile updated = userDetailsService.updateUserProfile(userDetails.getUsername(), profileDto);
        return ResponseEntity.ok(updated);
    }
}
