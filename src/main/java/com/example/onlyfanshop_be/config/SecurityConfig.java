package com.example.onlyfanshop_be.config;

import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.security.CustomUserDetailsService;
import com.example.onlyfanshop_be.security.JwtAuthenticationFilter;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenRepository tokenRepository; // ✅ thêm repository

    public SecurityConfig(JwtTokenProvider tokenProvider,
                          CustomUserDetailsService userDetailsService,
                          TokenRepository tokenRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenRepository = tokenRepository;
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
                                "category/public/**",
                                "brands/public/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
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
