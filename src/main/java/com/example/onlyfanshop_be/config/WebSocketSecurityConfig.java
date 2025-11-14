package com.example.onlyfanshop_be.config;

import com.example.onlyfanshop_be.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract JWT token from headers
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");
                    
                    if (authHeaders != null && !authHeaders.isEmpty()) {
                        String authHeader = authHeaders.get(0);
                        
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            
                            try {
                                // Validate token
                                if (jwtTokenProvider.validateToken(token)) {
                                    String email = jwtTokenProvider.getEmailFromJWT(token);
                                    Long userId = Long.parseLong(jwtTokenProvider.getUserIdFromJWT(token).toString());
                                    
                                    // Load user details
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                    
                                    // Create authentication with user ID in details
                                    java.util.Map<String, Object> details = new java.util.HashMap<>();
                                    details.put("userId", userId.toString());
                                    details.put("email", email);
                                    
                                    UsernamePasswordAuthenticationToken authentication = 
                                        new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                        );
                                    authentication.setDetails(details);
                                    
                                    // Set principal
                                    accessor.setUser(authentication);
                                    
                                    log.info("WebSocket authenticated user: {} (ID: {})", email, userId);
                                } else {
                                    log.warn("Invalid JWT token in WebSocket connection");
                                }
                            } catch (Exception e) {
                                log.error("Error authenticating WebSocket connection: " + e.getMessage());
                            }
                        }
                    }
                }
                
                return message;
            }
        });
    }
}

