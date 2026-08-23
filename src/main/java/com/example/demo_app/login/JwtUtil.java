package com.example.demo_app.login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public static String generateToken(String username, Map<String, Object> extraClaims) {
        SecretKey key = Keys.hmacShaKeyFor(
                (secretKey + "demo-app-jwt-secret".getBytes(StandardCharsets.UTF_8))
        );

        return Jwts.builder()
                .setSubject(username)
                .addClaims(extraClaims != null ? extraClaims : new HashMap<>())
                .signWith(key, io.jsonwebtoken.security.SignatureAlgorithm.HS256)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) // 1 hour
                .compact();
    }

    public static Claims getClaims(String token) {
        try {
            return Jwts.parser().build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public static boolean validateToken(String token, String username) {
        try {
            Claims claims = getClaims(token);
            return claims.getSubject().equals(username);
        } catch (Exception e) {
            return false;
        }
    }
}
