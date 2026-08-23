package com.example.demo_app.login;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private AuthenticationManager authManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromHeader(request);
            if (token != null && JwtUtil.validateToken(token)) {
                Claims claims = JwtUtil.getClaims(token);
                var username = claims.getSubject();

                // Re-authenticate with the token to get proper authorities
                Authentication authentication = authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, "") // no password needed for JWT
                                .setDetails(new org.springframework.security.authentication.AbstractAuthenticationDetails() {
                                    @Override public Object getPrincipal() { return username; }
                                    @Override public void setAuthenticated(boolean authenticated) {}
                                })
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Token is invalid, skip authentication check and pass through
        }
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        return null;
    }
}

