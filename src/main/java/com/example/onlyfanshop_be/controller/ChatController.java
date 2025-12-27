package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.ChatRoomDTO;
import com.example.onlyfanshop_be.dto.MessageDTO;
import com.example.onlyfanshop_be.dto.request.CreateChatRoomFromProductRequest;
import com.example.onlyfanshop_be.dto.request.CreateChatRoomRequest;
import com.example.onlyfanshop_be.dto.request.SendMessageRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Controller", description = "APIs for chat functionality")
public class ChatController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/messages")
    @Operation(summary = "Send a message", description = "Send a message to a chat room")
    public ResponseEntity<ApiResponse<String>> sendMessage(
            @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = jwtTokenProvider.extractToken(httpRequest);
            String senderId = jwtTokenProvider.getUserIdFromJWT(token).toString();
            
            chatService.sendMessage(request, senderId);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Message sent successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error sending message: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .statusCode(400)
                    .message("Failed to send message: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/rooms")
    @Operation(summary = "Get chat rooms", description = "Get list of chat rooms for admin, staff or customer")
    public ResponseEntity<ApiResponse<List<ChatRoomDTO>>> getChatRooms(HttpServletRequest httpRequest) {
        try {
            String token = jwtTokenProvider.extractToken(httpRequest);
            String userRole = jwtTokenProvider.getRoleFromJWT(token);
            String userId = jwtTokenProvider.getUserIdFromJWT(token).toString();
            
            List<ChatRoomDTO> chatRooms;
            
            if ("ADMIN".equals(userRole)) {
                chatRooms = chatService.getChatRoomsForAdmin();
            } else if ("STAFF".equals(userRole)) {
                chatRooms = chatService.getChatRoomsForStaff(userId);
            } else {
                // Customer chỉ có thể xem room của mình
                chatRooms = chatService.getChatRoomsForCustomer(userId);
            }
            
            return ResponseEntity.ok(ApiResponse.<List<ChatRoomDTO>>builder()
                    .statusCode(200)
                    .message("Chat rooms retrieved successfully")
                    .data(chatRooms)
                    .build());
        } catch (Exception e) {
            log.error("Error getting chat rooms: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<List<ChatRoomDTO>>builder()
                    .statusCode(400)
                    .message("Failed to get chat rooms: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "Get messages for a room", description = "Get all messages in a specific chat room")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getMessagesForRoom(
            @PathVariable String roomId,
            HttpServletRequest httpRequest) {
        try {
            String token = jwtTokenProvider.extractToken(httpRequest);
            String userId = jwtTokenProvider.getUserIdFromJWT(token).toString();
            String userRole = jwtTokenProvider.getRoleFromJWT(token);
            
            // Kiểm tra quyền truy cập room
            if (!hasAccessToRoom(roomId, userId, userRole)) {
                return ResponseEntity.badRequest().body(ApiResponse.<List<MessageDTO>>builder()
                        .statusCode(400)
                        .message("Access denied to this chat room")
                        .build());
            }
            
            List<MessageDTO> messages = chatService.getMessagesForRoom(roomId);
            
            return ResponseEntity.ok(ApiResponse.<List<MessageDTO>>builder()
                    .statusCode(200)
                    .message("Messages retrieved successfully")
                    .data(messages)
                    .build());
        } catch (Exception e) {
            log.error("Error getting messages: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<List<MessageDTO>>builder()
                    .statusCode(400)
                    .message("Failed to get messages: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/rooms/{roomId}/read")
    @Operation(summary = "Mark messages as read", description = "Mark all messages in a room as read")
    public ResponseEntity<ApiResponse<String>> markMessagesAsRead(
            @PathVariable String roomId,
            HttpServletRequest httpRequest) {
        try {
            String token = jwtTokenProvider.extractToken(httpRequest);
            String userId = jwtTokenProvider.getUserIdFromJWT(token).toString();
            
            chatService.markAllMessagesAsRead(roomId, userId);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Messages marked as read")
                    .build());
        } catch (Exception e) {
            log.error("Error marking messages as read: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .statusCode(400)
                    .message("Failed to mark messages as read: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/rooms/customer")
    @Operation(summary = "Get or create customer chat room", description = "Get or create chat room for customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> getOrCreateCustomerRoom(HttpServletRequest httpRequest) {
        try {
            log.info("Received request to get/create customer room");
            String token = jwtTokenProvider.extractToken(httpRequest);
            log.info("Token extracted successfully");
            
            String customerId = jwtTokenProvider.getUserIdFromJWT(token).toString();
            log.info("Customer ID: " + customerId);
            
            // Use ChatService to get/create room with Firebase
            String roomId = chatService.getOrCreateChatRoom(customerId);
            log.info("Room ID created/retrieved: " + roomId);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("Chat room retrieved/created successfully")
                    .data(roomId)
                    .build());
        } catch (Exception e) {
            log.error("Error getting/creating customer room: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .statusCode(400)
                    .message("Failed to get/create customer room: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/rooms/from-product")
    @Operation(summary = "Create chat room from product", description = "Create a chat room for customer to chat with staff about a product")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> createChatRoomFromProduct(
            @RequestBody CreateChatRoomFromProductRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = jwtTokenProvider.extractToken(httpRequest);
            String customerId = jwtTokenProvider.getUserIdFromJWT(token).toString();
            
            String roomId = chatService.createChatRoomFromProduct(customerId, request);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(201)
                    .message("Chat room created successfully")
                    .data(roomId)
                    .build());
        } catch (Exception e) {
            log.error("Error creating chat room from product: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .statusCode(400)
                    .message("Failed to create chat room: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testEndpoint() {
        log.info("Test endpoint called");
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .statusCode(200)
                .message("Test endpoint working")
                .data("test_data")
                .build());
    }

    
    @PostMapping("/clear-all-chat-data-public")
    public ResponseEntity<ApiResponse<String>> clearAllChatDataPublic() {
        try {
            log.info("Clearing all chat data...");
            // Clear all chat rooms
            chatService.clearAllChatRooms();
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .statusCode(200)
                    .message("All chat data cleared successfully")
                    .data("All chat data cleared")
                    .build());
        } catch (Exception e) {
            log.error("Error clearing chat data: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .statusCode(400)
                    .message("Failed to clear chat data: " + e.getMessage())
                    .build());
        }
    }


    private boolean hasAccessToRoom(String roomId, String userId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return true; // Admin có thể truy cập tất cả rooms
        } else {
            // Customer chỉ có thể truy cập room của mình
            return roomId.equals("room_" + userId) || 
                   roomId.contains("_" + userId);
        }
    }
}
