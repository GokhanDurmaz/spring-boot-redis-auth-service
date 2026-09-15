package com.example.demo_app.service;

import com.example.demo_app.dto.UserProfile;
import com.example.demo_app.entity.User;
import com.example.demo_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }

    @Cacheable(value = "users", key = "#username")
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public UserProfile getUserProfile(String username) {
        User user = findUserByUsername(username);
        return UserProfile.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build();
    }

    @Transactional
    @CacheEvict(value = "users", key = "#username") // Clean cache for new inputs
    public UserProfile updateUserProfile(String username, UserProfile dto) {
        User user = findUserByUsername(username);

        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }

        User updatedUser = userRepository.save(user);

        return UserProfile.builder()
                .username(updatedUser.getUsername())
                .fullName(updatedUser.getFullName())
                .email(updatedUser.getEmail())
                .avatarUrl(updatedUser.getAvatarUrl())
                .bio(updatedUser.getBio())
                .build();
    }
}
