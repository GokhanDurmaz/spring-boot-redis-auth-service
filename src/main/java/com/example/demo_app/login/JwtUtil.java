package com.example.demo_app.login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

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

    private static final String SECRET = "demo-app-jwt-secret-key-must-be-at-least-32-bytes-long!";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_TIME = 86400000; // 24 hour

    public static String generateToken(String username, Object claims) {
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
                .signWith(KEY)
                .compact();
    }

    public static Boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
