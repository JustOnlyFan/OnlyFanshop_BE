package com.example.onlyfanshop_be.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final class Counter { long count; Instant windowStart; }
    private final Map<String, Counter> ipCounters = new ConcurrentHashMap<>();

    @Value("${rate.login.windowSeconds:60}")
    private long windowSeconds;
    @Value("${rate.login.maxRequests:20}")
    private long maxRequests;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("/login/signin".equals(path)) {
            String ip = request.getRemoteAddr();
            Instant now = Instant.now();
            Counter c = ipCounters.computeIfAbsent(ip, k -> {
                Counter nc = new Counter();
                nc.count = 0;
                nc.windowStart = now;
                return nc;
            });
            synchronized (c) {
                if (now.isAfter(c.windowStart.plusSeconds(windowSeconds))) {
                    c.windowStart = now;
                    c.count = 0;
                }
                c.count++;
                if (c.count > maxRequests) {
                    response.setStatus(429);
                    response.getWriter().write("Too Many Requests");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}














