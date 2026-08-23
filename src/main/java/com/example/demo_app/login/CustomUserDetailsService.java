package com.example.demo_app.login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        // Return a UserDetails object without the password hash (already hashed in DB)
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password("") // empty — already verified via JWT token
                .roles("USER").build();
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}

