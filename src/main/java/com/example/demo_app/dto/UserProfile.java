package com.example.demo_app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String bio;
}
