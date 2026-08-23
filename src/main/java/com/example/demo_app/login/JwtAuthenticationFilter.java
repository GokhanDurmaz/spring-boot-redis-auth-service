package com.example.demo_app.login;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private AuthenticationManager authManager;
    @Autowired private CustomUserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromHeader(request);
            if (token != null && JwtUtil.validateToken(token)) {
                Claims claims = JwtUtil.getClaims(token);
                var username = claims.getSubject();

                // Re-authenticate with the token to get proper authorities
                Authentication authentication = authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, "")
                                .setPrincipal(username)
                                .setDetails(new org.springframework.security.authentication.AbstractUserDetails() {
                                    @Override public Object getPassword() { return null; }
                                    @Override public String getUsername() { return username; }
                                    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
                                        var user = userDetailsService.loadUserByUsername(username);
                                        return user.getAuthorities();
                                    }
                                })
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Token is invalid, skip authentication check and pass through
            filterChain.doFilter(request, response);
        }
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
