package com.example.onlyfanshop_be.security;

import com.example.onlyfanshop_be.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class JwtTokenProvider {
    @Value("${JWT_SECRET}")
    private String JWT_SECRET;
    
    @Value("${jwt.access.ttlMinutes:30}")
    public long accessTtlMinutes;
    
    @Value("${jwt.refresh.ttlDays:7}")
    public long refreshTtlDays;
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    public String generateAccessToken(String email, Long userId, Role role, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTtlMinutes, ChronoUnit.MINUTES);
        String roleName = role != null ? role.getName() : "customer";
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .addClaims(Map.of(
                        "userId", userId,
                        "role", roleName,
                        "username", username != null ? username : ""
                ))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String email, Long userId, Role role) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTtlDays, ChronoUnit.DAYS);
        String roleName = role != null ? role.getName() : "customer";
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .addClaims(Map.of(
                        "userId", userId,
                        "role", roleName
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

    public Long getUserIdFromJWT(String token) {
        Object userId = getAllClaimsFromToken(token).get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return Long.parseLong(userId.toString());
    }
    
    // Legacy method for backward compatibility
    @Deprecated
    public Integer getUserIdFromJWTAsInteger(String token) {
        return getUserIdFromJWT(token).intValue();
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

    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new RuntimeException("Token không hợp lệ hoặc không được cung cấp!");
    }
}
