package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.MessageDTO;
import com.example.onlyfanshop_be.dto.request.SendMessageRequest;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload SendMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Principal principal = headerAccessor.getUser();
            if (principal == null) {
                log.warn("Unauthenticated WebSocket message attempt");
                return;
            }

            Authentication auth = (Authentication) principal;
            String senderId = extractUserIdFromAuth(auth);
            
            if (senderId == null) {
                log.warn("Could not extract user ID from authentication");
                return;
            }

            log.info("WebSocket message received from user {} in room {}", senderId, request.getRoomId());

            // WebSocket handles real-time delivery, Firebase stores for history
            chatService.sendMessageViaWebSocket(request, senderId);

            // Create message DTO for real-time broadcast
            MessageDTO messageDTO = MessageDTO.builder()
                    .roomId(request.getRoomId())
                    .senderId(senderId)
                    .message(request.getMessage())
                    .timestamp(LocalDateTime.now())
                    .epochMillis(System.currentTimeMillis())
                    .isRead(false)
                    .build();

            // Broadcast to all subscribers of this room
            messagingTemplate.convertAndSend("/topic/chat/" + request.getRoomId(), messageDTO);

        } catch (Exception e) {
            log.error("Error handling WebSocket message: " + e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public void addUser(@Payload String roomId, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Principal principal = headerAccessor.getUser();
            if (principal == null) {
                return;
            }

            Authentication auth = (Authentication) principal;
            String userId = extractUserIdFromAuth(auth);
            
            if (userId != null) {
                log.info("User {} joined room {}", userId, roomId);
                // Add user to session attributes
                headerAccessor.getSessionAttributes().put("userId", userId);
                headerAccessor.getSessionAttributes().put("roomId", roomId);
            }
        } catch (Exception e) {
            log.error("Error adding user to room: " + e.getMessage(), e);
        }
    }

    private String extractUserIdFromAuth(Authentication auth) {
        try {
            String email = auth.getName();

            if (auth.getDetails() instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
                if (details.containsKey("userId")) {
                    return details.get("userId").toString();
                }
            }
            return email;
        } catch (Exception e) {
            log.error("Error extracting user ID: " + e.getMessage());
        }
        return null;
    }
}

