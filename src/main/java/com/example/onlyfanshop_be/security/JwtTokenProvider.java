package com.example.onlyfanshop_be.security;

import com.example.onlyfanshop_be.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {
    @Value("${JWT_SECRET}")
    private String JWT_SECRET;
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    public String generateToken(String email, int userId, Role role, String username) {
        Date now = new Date();

        return Jwts.builder()
                .setSubject(email) // sub = email
                .setIssuedAt(now)
                .addClaims(Map.of(
                        "userId", userId,
                        "role", role,
                        "username", username
                ))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public String getEmailFromJWT(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public Integer getUserIdFromJWT(String token) {
        return getAllClaimsFromToken(token).get("userId", Integer.class);
    }

    public String getRoleFromJWT(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }


    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
