package com.example.onlyfanshop_be.security;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.service.ILoginService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String X_NEW_ACCESS_TOKEN_HEADER = "X-New-Access-Token";
    private static final long MINUTES_BEFORE_EXPIRATION = 5; // Refresh token if it expires within 5 minutes

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;
    private final ILoginService loginService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService,
                                   TokenRepository tokenRepository,
                                   ILoginService loginService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenRepository = tokenRepository;
        this.loginService = loginService;
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
                log.warn("Invalid or expired JWT token for request: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"statusCode\":401,\"message\":\"Token is invalid or expired. Please login again.\"}");
                return;
            }

            // Check DB status (revocation/expiry)
            java.util.Optional<com.example.onlyfanshop_be.entity.Token> dbTokenOpt = tokenRepository.findByToken(token);
            if (dbTokenOpt.isPresent()) {
                com.example.onlyfanshop_be.entity.Token dbToken = dbTokenOpt.get();
                if (dbToken.isRevoked()) {
                    log.warn("Token is revoked for request: {}", requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"statusCode\":401,\"message\":\"Token has been revoked. Please login again.\"}");
                    return;
                }
                // Check if token is expired in DB (even if JWT itself is still valid)
                if (dbToken.getExpiresAt() != null && dbToken.getExpiresAt().isBefore(java.time.Instant.now())) {
                    log.warn("Token is expired in DB for request: {}", requestURI);
                    dbToken.setExpired(true);
                    tokenRepository.save(dbToken);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"statusCode\":401,\"message\":\"Token has expired. Please login again.\"}");
                    return;
                }
                if (dbToken.isExpired()) {
                    log.warn("Token is marked as expired in DB for request: {}", requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"statusCode\":401,\"message\":\"Token has expired. Please login again.\"}");
                    return;
                }
            } else {
                // Token not found in DB - this could be a legacy token or invalid token
                log.warn("Token not found in database for request: {}. Rejecting request for security.", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"statusCode\":401,\"message\":\"Token not found. Please login again.\"}");
                return;
            }

            try {
                String username = tokenProvider.getUsernameFromJWT(token);
                if (username == null || username.isEmpty()) {
                    log.warn("JWT does not contain a valid username for request: {}", requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"statusCode\":401,\"message\":\"Invalid token. Please login again.\"}");
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (userDetails == null) {
                    log.warn("User not found for username extracted from JWT: {}", username);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"statusCode\":401,\"message\":\"User not found. Please login again.\"}");
                    return;
                }

                if (userDetails.getAuthorities() == null) {
                    log.warn("UserDetails has null authorities for username: {}", username);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"statusCode\":401,\"message\":\"Invalid user permissions. Please login again.\"}");
                    return;
                }

                // Log authorities before creating authentication
                System.out.println("JwtAuthenticationFilter: UserDetails authorities: " + userDetails.getAuthorities());
                for (var authority : userDetails.getAuthorities()) {
                    System.out.println("JwtAuthenticationFilter: Authority: " + authority.getAuthority());
                }
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Verify authentication was set
                var contextAuth = SecurityContextHolder.getContext().getAuthentication();
                System.out.println("JwtAuthenticationFilter: SecurityContext authentication set: " + (contextAuth != null));
                if (contextAuth != null) {
                    System.out.println("JwtAuthenticationFilter: SecurityContext authorities: " + contextAuth.getAuthorities());
                }
                
                log.info("Authentication successful for user: {} with authorities: {} on request: {}",
                        username, userDetails.getAuthorities(), requestURI);

                // Check if token is expiring soon and auto-refresh it
                try {
                    if (tokenProvider.isTokenExpiringSoon(token, MINUTES_BEFORE_EXPIRATION)) {
                        log.info("Token is expiring soon (within {} minutes), attempting to refresh...", MINUTES_BEFORE_EXPIRATION);
                        String refreshToken = getRefreshTokenFromCookie(request);
                        if (refreshToken != null && !refreshToken.isEmpty()) {
                            try {
                                ApiResponse<UserDTO> refreshResponse = loginService.refreshToken(refreshToken);
                                if (refreshResponse != null && refreshResponse.getData() != null && refreshResponse.getData().getToken() != null) {
                                    String newAccessToken = refreshResponse.getData().getToken();
                                    // Add new token to response header for frontend to pick up
                                    response.setHeader(X_NEW_ACCESS_TOKEN_HEADER, newAccessToken);
                                    log.info("Token refreshed successfully for user: {}", username);
                                }
                            } catch (Exception refreshError) {
                                log.warn("Failed to refresh token for user: {}, error: {}", username, refreshError.getMessage());
                                // Don't fail the request if refresh fails - token is still valid
                            }
                        } else {
                            log.warn("Token is expiring soon but no refresh token found in cookie");
                        }
                    }
                } catch (Exception refreshCheckError) {
                    log.warn("Error checking token expiration: {}", refreshCheckError.getMessage());
                    // Don't fail the request if expiration check fails
                }

            } catch (Exception e) {
                log.error("Error setting authentication: ", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                try {
                    response.getWriter().write("{\"statusCode\":401,\"message\":\"Authentication failed. Please login again.\"}");
                } catch (IOException ioException) {
                    log.error("Error writing error response", ioException);
                }
                return;
            }

        }

        filterChain.doFilter(request, response);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie refreshCookie = WebUtils.getCookie(request, REFRESH_COOKIE_NAME);
        if (refreshCookie != null) {
            return refreshCookie.getValue();
        }
        return null;
    }
}
