package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ChatRoomDTO;
import com.example.onlyfanshop_be.dto.MessageDTO;
import com.example.onlyfanshop_be.dto.request.CreateChatRoomRequest;
import com.example.onlyfanshop_be.dto.request.SendMessageRequest;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.google.firebase.database.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final DatabaseReference databaseReference;
    private final FCMService fcmService;
    private final UserRepository userRepository;

    public String createChatRoom(CreateChatRoomRequest request, String adminId) {
        // Lấy thông tin customer để tạo room ID
        User customer = userRepository.findById(Integer.parseInt(request.getCustomerId()))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        String roomId = "chatRoom_" + customer.getUsername() + "_" + request.getCustomerId();

        // Tạo conversation trong Firebase
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("participants/admin", true);
        roomData.put("participants/" + request.getCustomerId(), true);
        roomData.put("createdAt", System.currentTimeMillis());
        roomData.put("lastMessage", request.getInitialMessage());
        roomData.put("lastMessageTime", System.currentTimeMillis());

        databaseReference.child("Conversations").child(roomId).setValueAsync(roomData);

        // Gửi tin nhắn đầu tiên nếu có
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            sendMessage(SendMessageRequest.builder()
                    .roomId(roomId)
                    .message(request.getInitialMessage())
                    .build(), request.getCustomerId());
        }

        return roomId;
    }

    public void sendMessage(SendMessageRequest request, String senderId) {
        String messageId = databaseReference.child("Messages").child(request.getRoomId()).push().getKey();
        
        // Lấy thông tin người gửi
        User sender = userRepository.findById(Integer.parseInt(senderId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("senderName", sender.getUsername());
        messageData.put("message", request.getMessage());
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("isRead", false);
        
        if (request.getAttachmentUrl() != null) {
            messageData.put("attachmentUrl", request.getAttachmentUrl());
            messageData.put("attachmentType", request.getAttachmentType());
        }
        
        if (request.getReplyToMessageId() != null) {
            messageData.put("replyToMessageId", request.getReplyToMessageId());
        }
        
        // Lưu tin nhắn vào Firebase
        databaseReference.child("Messages").child(request.getRoomId()).child(messageId)
                .setValueAsync(messageData);
        
        // Cập nhật lastMessage và lastMessageTime
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastMessage", request.getMessage());
        updateData.put("lastMessageTime", System.currentTimeMillis());
        
        databaseReference.child("Conversations").child(request.getRoomId()).updateChildrenAsync(updateData);
        
        // Gửi FCM notification
        sendNotificationToOtherParticipants(request.getRoomId(), senderId, sender.getUsername(), request.getMessage());
    }

        public List<ChatRoomDTO> getChatRoomsForAdmin() {
        try {
            CompletableFuture<List<ChatRoomDTO>> future = new CompletableFuture<>();
            
            databaseReference.child("Conversations").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<ChatRoomDTO> rooms = new ArrayList<>();
                    
                    if (snapshot.exists()) {
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            try {
                                String roomId = roomSnapshot.getKey();
                                
                                // Lấy thông tin participants
                                Map<String, Boolean> participants = new HashMap<>();
                                DataSnapshot participantsSnapshot = roomSnapshot.child("participants");
                                
                                for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                                    participants.put(participant.getKey(), true);
                                }
                                
                                // Lấy thông tin customer (không phải admin, không phải Firebase UID dài)
                                String customerId = participants.keySet().stream()
                                        .filter(id -> !id.equals("admin") && id.length() < 20)
                                        .findFirst()
                                        .orElse(null);
                                
                                if (customerId != null) {
                                    String lastMessage = roomSnapshot.child("lastMessage").getValue(String.class);
                                    Long lastMessageTime = roomSnapshot.child("lastMessageTime").getValue(Long.class);
                                    
                                    // ✅ Fix: Extract customerName từ roomId thay vì query database
                                    // Room ID format: chatRoom_username_userId (ví dụ: chatRoom_huy_4)
                                    // Điều này đảm bảo luôn đúng ngay cả khi dùng chung account ID ở database local khác nhau
                                    String customerName = extractCustomerNameFromRoomId(roomId);
                                    if (customerName == null || customerName.isEmpty()) {
                                        // Fallback: Thử lấy từ database nếu không extract được từ roomId
                                        User customer = getCachedUser(customerId);
                                        customerName = customer != null ? customer.getUsername() : "Customer " + customerId;
                                    }
                                    
                                    ChatRoomDTO roomDTO = ChatRoomDTO.builder()
                                            .roomId(roomId)
                                            .participants(participants)
                                            .lastMessage(lastMessage != null ? lastMessage : "No messages yet")
                                            .lastMessageTime(lastMessageTime != null ? 
                                                LocalDateTime.ofEpochSecond(lastMessageTime / 1000, 0, ZoneOffset.UTC) : 
                                                LocalDateTime.now())
                                            .customerName(customerName)
                                            .customerAvatar(null)
                                            .isOnline(false)
                                            .unreadCount(0)
                                            .build();
                                    
                                    rooms.add(roomDTO);
                                }
                            } catch (Exception e) {
                                log.error("Error processing room: " + e.getMessage());
                            }
                        }
                        
                        rooms.sort((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()));
                    }
                    
                    future.complete(rooms);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    log.error("Error getting chat rooms: " + error.getMessage());
                    future.completeExceptionally(new RuntimeException("Failed to get chat rooms"));
                }
            });
            
            return future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("Error getting chat rooms: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // Cache để tránh query database nhiều lần
    private final Map<String, User> userCache = new HashMap<>();
    
    private User getCachedUser(String userId) {
        if (userCache.containsKey(userId)) {
            return userCache.get(userId);
        }
        
        try {
            User user = userRepository.findById(Integer.parseInt(userId)).orElse(null);
            if (user != null) {
                userCache.put(userId, user);
            }
            return user;
        } catch (Exception e) {
            log.error("Error getting user: " + e.getMessage());
            return null;
        }
    }
    
    // ✅ Extract customer name từ roomId
    // Room ID format: chatRoom_username_userId (ví dụ: chatRoom_huy_4)
    private String extractCustomerNameFromRoomId(String roomId) {
        try {
            if (roomId != null && roomId.startsWith("chatRoom_")) {
                String[] parts = roomId.split("_");
                // parts[0] = "chatRoom"
                // parts[1] = username (e.g., "huy", "NTT")
                // parts[2] = userId (e.g., "4")
                if (parts.length >= 3) {
                    return parts[1]; // Return username
                } else if (parts.length == 2) {
                    // Fallback for old format: chatRoom_userId
                    return null; // Cannot extract username, return null to use fallback
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting customer name from room ID: " + e.getMessage());
            return null;
        }
    }

    
    
    
    public void clearAllChatRooms() {
        log.info("Clearing all chat rooms...");
        try {
            databaseReference.child("Conversations").removeValueAsync();
            databaseReference.child("Messages").removeValueAsync();
            log.info("All chat rooms cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing chat rooms: " + e.getMessage(), e);
        }
    }

        public List<MessageDTO> getMessagesForRoom(String roomId) {
        CompletableFuture<List<MessageDTO>> future = new CompletableFuture<>();
        
        databaseReference.child("Messages").child(roomId)
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<MessageDTO> messageList = new ArrayList<>();
                        
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            String messageId = messageSnapshot.getKey();
                            String senderId = messageSnapshot.child("senderId").getValue(String.class);
                            String senderName = messageSnapshot.child("senderName").getValue(String.class);
                            String message = messageSnapshot.child("message").getValue(String.class);
                            Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                            String attachmentUrl = messageSnapshot.child("attachmentUrl").getValue(String.class);
                            String attachmentType = messageSnapshot.child("attachmentType").getValue(String.class);
                            String replyToMessageId = messageSnapshot.child("replyToMessageId").getValue(String.class);
                            Boolean isRead = messageSnapshot.child("isRead").getValue(Boolean.class);
                            
                            MessageDTO messageDTO = MessageDTO.builder()
                                    .messageId(messageId)
                                    .senderId(senderId)
                                    .senderName(senderName)
                                    .message(message)
                                    .epochMillis(timestamp)
                                    .timestamp(timestamp != null ? LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC) : LocalDateTime.now())
                                    .attachmentUrl(attachmentUrl)
                                    .attachmentType(attachmentType)
                                    .replyToMessageId(replyToMessageId)
                                    .isRead(isRead != null ? isRead : false)
                                    .roomId(roomId)
                                    .build();
                            
                            messageList.add(messageDTO);
                        }
                        
                        future.complete(messageList);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("Error getting messages: " + error.getMessage());
                        future.completeExceptionally(new RuntimeException("Failed to get messages"));
                    }
                });
        
        try {
            return future.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting messages: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get messages", e);
        }
    }

    public String getOrCreateChatRoom(String customerId) {
        User customer = userRepository.findById(Integer.parseInt(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        String roomId = "chatRoom_" + customer.getUsername() + "_" + customerId;

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        databaseReference.child("Conversations").child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> roomData = new HashMap<>();
                    Map<String, Boolean> participants = new HashMap<>();
                    participants.put("admin", true);
                    participants.put(customerId, true);
                    roomData.put("participants", participants);
                    roomData.put("createdAt", System.currentTimeMillis());
                    roomData.put("lastMessage", "Chat started");
                    roomData.put("lastMessageTime", System.currentTimeMillis());
                    databaseReference.child("Conversations").child(roomId).setValueAsync(roomData);
                }
                future.complete(true);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Error checking chat room: " + error.getMessage());
                future.completeExceptionally(new RuntimeException("Failed to check chat room"));
            }
        });

        try {
            future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            return roomId;
        } catch (Exception e) {
            log.warn("Timeout/error creating chat room: " + e.getMessage());
            return roomId;
        }
    }

    private void sendNotificationToOtherParticipants(String roomId, String senderId, String senderName, String message) {
        // Lấy danh sách participants
        databaseReference.child("Conversations").child(roomId).child("participants")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot participant : snapshot.getChildren()) {
                            String participantId = participant.getKey();
                            
                            // Không gửi notification cho người gửi
                            if (!participantId.equals(senderId)) {
                                // ✅ Handle "admin" participant ID - skip FCM notification for admin
                                if ("admin".equals(participantId)) {
                                    log.info("Skipping FCM notification for admin participant");
                                    continue;
                                }
                                
                                // ✅ Only parse numeric participant IDs
                                try {
                                    Integer userId = Integer.parseInt(participantId);
                                    userRepository.findById(userId)
                                            .ifPresent(user -> {
                                                if (user.getFcmToken() != null) {
                                                    fcmService.sendChatNotification(
                                                            user.getFcmToken(),
                                                            senderName,
                                                            message,
                                                            roomId
                                                    );
                                                }
                                            });
                                } catch (NumberFormatException e) {
                                    log.warn("Skipping non-numeric participant ID: " + participantId);
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("Error getting participants: " + error.getMessage());
                    }
                });
    }

    public void markMessageAsRead(String roomId, String messageId) {
        databaseReference.child("Messages").child(roomId).child(messageId)
                .child("isRead").setValueAsync(true);
    }

    public void markAllMessagesAsRead(String roomId, String userId) {
        databaseReference.child("Messages").child(roomId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            String senderId = messageSnapshot.child("senderId").getValue(String.class);
                            
                            // Chỉ đánh dấu đã đọc tin nhắn của người khác
                            if (!senderId.equals(userId)) {
                                messageSnapshot.getRef().child("isRead").setValueAsync(true);
                            }
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("Error marking messages as read: " + error.getMessage());
                    }
                });
    }
}
