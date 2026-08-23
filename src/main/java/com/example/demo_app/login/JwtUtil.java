package com.example.demo_app.login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    @Value("${jwt-secret}")
    private String secretKey;

    public static String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .addClaims(new HashMap<>())
                .signWith(io.jsonwebtoken.security.Keys.secretKeyForSha256(secretKey.getBytes()), io.jsonwebtoken.SignatureAlgorithm.RS256)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) // 1 hour
                .signWith(io.jsonwebtoken.security.Keys.secretKeyForSha256(secretKey.getBytes()), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder().build().parseClaimsJws(token).getBody();
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

    @Value("${jwt.secret-key}")
    private String secretKey;
}
