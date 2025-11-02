package com.example.onlyfanshop_be.security;

import com.example.onlyfanshop_be.repository.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService,
                                   TokenRepository tokenRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);

            // Validate JWT signature and expiration
            if (!tokenProvider.validateToken(token)) {
                log.warn("Invalid JWT token for request: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            // Check DB status (revocation/expiry) but do not block if token is missing in DB
            java.util.Optional<com.example.onlyfanshop_be.entity.Token> dbTokenOpt = tokenRepository.findByToken(token);
            if (dbTokenOpt.isPresent()) {
                com.example.onlyfanshop_be.entity.Token dbToken = dbTokenOpt.get();
                if (dbToken.isRevoked()) {
                    log.warn("Token is revoked for request: {}", requestURI);
                    filterChain.doFilter(request, response);
                    return;
                }
                if (dbToken.isExpired()) {
                    log.warn("Token is expired in DB for request: {}", requestURI);
                    filterChain.doFilter(request, response);
                    return;
                }
            } else {
                // Allow flow to continue to avoid false negatives if DB mismatch; still logged for visibility
                log.warn("Token not found in database for request: {}. Proceeding because JWT is valid (dev-safe path).", requestURI);
            }

            try {
                String username = tokenProvider.getUsernameFromJWT(token);
                if (username == null || username.isEmpty()) {
                    log.warn("JWT does not contain a valid username for request: {}", requestURI);
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (userDetails == null) {
                    log.warn("User not found for username extracted from JWT: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (userDetails.getAuthorities() == null) {
                    log.warn("UserDetails has null authorities for username: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication successful for user: {} with role: {} on request: {}",
                        username, userDetails.getAuthorities(), requestURI);

            } catch (Exception e) {
                log.error("Error setting authentication: ", e);
            }

        }

        filterChain.doFilter(request, response);
    }
}
