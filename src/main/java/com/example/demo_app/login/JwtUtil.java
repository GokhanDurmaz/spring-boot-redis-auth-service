package com.example.demo_app.login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey key;
    private static final long EXPIRATION_TIME = 86400000; // 24 hour

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, Object claims) {
        Map<String, Object> claimsMap = new HashMap<>();
        
        if (claims != null) {
            if (claims instanceof Collection<?> collection) {
                List<String> roleNames = collection.stream()
                        .map(item -> {
                            if (item instanceof GrantedAuthority authority) {
                                return authority.getAuthority();
                            }
                            return item.toString();
                        })
                        .toList();
                claimsMap.put("roles", roleNames);
            } else {
                claimsMap.put("roles", claims);
            }
        }

        return Jwts.builder()
                .claims(claimsMap)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(this.key)
                .compact();
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
