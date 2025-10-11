package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.entity.ChatMessage;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.ChatMessageRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Sync message from Firebase to MySQL database
     */
    @PostMapping("/sync-message")
    public ResponseEntity<ApiResponse<String>> syncMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam String message,
            @RequestParam Long timestamp) {
        
        try {
            log.info("Syncing message from Firebase: sender={}, receiver={}, message={}", 
                    senderId, receiverId, message);

            // Find sender and receiver users
            Optional<User> sender = userRepository.findByEmail(senderId);
            Optional<User> receiver = userRepository.findByEmail(receiverId);
            
            if (sender.isEmpty() || receiver.isEmpty()) {
                log.warn("User not found: sender={}, receiver={}", senderId, receiverId);
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .statusCode(404)
                        .message("User not found")
                        .build());
            }

            // Create ChatMessage entity
            ChatMessage chatMessage = ChatMessage.builder()
                    .message(message)
                    .sentAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .sender(sender.get())
                    .receiver(receiver.get())
                    .build();

            // Save to database
            chatMessageRepository.save(chatMessage);
            
            log.info("Message synced successfully: ID={}", chatMessage.getChatMessageID());
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Message synced successfully")
                    .build());

        } catch (Exception e) {
            log.error("Error syncing message: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(500)
                    .message("Failed to sync message: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get chat messages between two users
     */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getMessages(
            @RequestParam Integer userId1,
            @RequestParam Integer userId2,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            log.info("Getting messages between users: {} and {}", userId1, userId2);
            
            // This would need to be implemented in repository
            // List<ChatMessage> messages = chatMessageRepository.findMessagesBetweenUsers(userId1, userId2, page, size);
            
            // For now, return empty list
            List<ChatMessage> messages = List.of();
            
            log.info("Retrieved {} messages", messages.size());
            return ResponseEntity.ok(ApiResponse.<List<ChatMessage>>builder()
                    .statusCode(200)
                    .message("Messages retrieved successfully")
                    .data(messages)
                    .build());

        } catch (Exception e) {
            log.error("Error getting messages: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<ChatMessage>>builder()
                    .statusCode(500)
                    .message("Failed to get messages: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Mark message as read
     */
    @PutMapping("/mark-read/{messageId}")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Integer messageId) {
        try {
            log.info("Marking message as read: {}", messageId);
            
            Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
            if (messageOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .statusCode(404)
                        .message("Message not found")
                        .build());
            }

            ChatMessage message = messageOpt.get();
            message.setUpdatedAt(LocalDateTime.now());
            chatMessageRepository.save(message);
            
            log.info("Message marked as read: {}", messageId);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Message marked as read")
                    .build());

        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(500)
                    .message("Failed to mark message as read: " + e.getMessage())
                    .build());
        }
    }
}
