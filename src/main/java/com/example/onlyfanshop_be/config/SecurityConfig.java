package com.example.onlyfanshop_be.config;

import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.security.CustomUserDetailsService;
import com.example.onlyfanshop_be.security.JwtAuthenticationFilter;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    public class SecurityConfig {

        private final JwtTokenProvider tokenProvider;
        private final CustomUserDetailsService userDetailsService;
        private final TokenRepository tokenRepository; // ✅ thêm repository

        private final LoginRateLimitFilter loginRateLimitFilter;

        public SecurityConfig(JwtTokenProvider tokenProvider,
                              CustomUserDetailsService userDetailsService,
                              TokenRepository tokenRepository,
                              LoginRateLimitFilter loginRateLimitFilter) {
            this.tokenProvider = tokenProvider;
            this.userDetailsService = userDetailsService;
            this.tokenRepository = tokenRepository;
            this.loginRateLimitFilter = loginRateLimitFilter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            // ✅ Truyền thêm tokenRepository vào filter
            JwtAuthenticationFilter jwtFilter =
                    new JwtAuthenticationFilter(tokenProvider, userDetailsService, tokenRepository);

            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        "/login/**",
                                        "/swagger-ui/**",
                                        "/api/auth/google/**",
                                        "/v3/api-docs/**",
                                        "/swagger-resources/**",
                                        "/swagger-resources",
                                        "/webjars/**",
                                        "/product/public/**",
                                        "/category/public/**",
                                        "/brands/public/**",
                                        "/payment/public/**",
                                        "/warranties/public/**",
                                        "/colors/public/**",
                                        "/api/chat/test",
                                        "/api/chat/clear-all-chat-data-public",
                                        "/users/token-status",
                                        "/store-locations",
                                        "/store-locations/**",
                                        "/ws/**",
                                        "/ws",
                                        "/api/webhooks/**",
                                        "/api/shipments/ghn/**",
                                        "/api/shipments/calculate-fee"
                                ).permitAll()
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(exceptions -> exceptions
                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                // Debug: Log authentication info
                                org.springframework.security.core.Authentication auth = 
                                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                                System.err.println("AccessDeniedHandler: User: " + (auth != null ? auth.getName() : "null"));
                                System.err.println("AccessDeniedHandler: Authorities: " + (auth != null ? auth.getAuthorities() : "null"));
                                System.err.println("AccessDeniedHandler: Exception: " + accessDeniedException.getMessage());
                                
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                String message = "Access Denied: " + accessDeniedException.getMessage();
                                if (auth != null) {
                                    message += ". User: " + auth.getName() + ", Authorities: " + auth.getAuthorities();
                                }
                                response.getWriter().write(
                                        "{\"statusCode\":403,\"message\":\"" + message + "\",\"dateTime\":\"" + 
                                        new java.util.Date() + "\"}"
                                );
                            })
                            .authenticationEntryPoint((request, response, authException) -> {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                        "{\"statusCode\":401,\"message\":\"Authentication required: " + 
                                        authException.getMessage() + "\",\"dateTime\":\"" + 
                                        new java.util.Date() + "\"}"
                                );
                            })
                    )
                    .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
            return authConfig.getAuthenticationManager();
        }
    }
