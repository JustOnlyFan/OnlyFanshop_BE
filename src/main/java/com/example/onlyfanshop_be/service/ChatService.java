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

        // Tạo chat room trong Firebase
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("participants/admin", true);
        roomData.put("participants/" + request.getCustomerId(), true);
        roomData.put("createdAt", System.currentTimeMillis());
        roomData.put("lastMessage", request.getInitialMessage());
        roomData.put("lastMessageTime", System.currentTimeMillis());

        databaseReference.child("ChatRooms").child(roomId).setValueAsync(roomData);

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
        String messageId = databaseReference.child("ChatRooms").child(request.getRoomId()).child("messages").push().getKey();
        
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
        databaseReference.child("ChatRooms").child(request.getRoomId()).child("messages").child(messageId)
                .setValueAsync(messageData);
        
        // Cập nhật lastMessage và lastMessageTime
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastMessage", request.getMessage());
        updateData.put("lastMessageTime", System.currentTimeMillis());
        
        databaseReference.child("ChatRooms").child(request.getRoomId()).updateChildrenAsync(updateData);
        
        // Gửi FCM notification
        sendNotificationToOtherParticipants(request.getRoomId(), senderId, sender.getUsername(), request.getMessage());
    }

        public List<ChatRoomDTO> getChatRoomsForAdmin() {
            log.info("Getting chat rooms for admin...");
        
        try {
            // Sử dụng timeout ngắn hơn để tránh timeout
            CompletableFuture<List<ChatRoomDTO>> future = new CompletableFuture<>();
            
            databaseReference.child("ChatRooms").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<ChatRoomDTO> rooms = new ArrayList<>();
                    
                    if (snapshot.exists()) {
                        log.info("Found " + snapshot.getChildrenCount() + " chat rooms");
                        
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            try {
                                String roomId = roomSnapshot.getKey();
                                log.info("Processing room: " + roomId);
                                
                                // Lấy thông tin participants
                                Map<String, Boolean> participants = new HashMap<>();
                                DataSnapshot participantsSnapshot = roomSnapshot.child("participants");
                                log.info("Participants snapshot exists: " + participantsSnapshot.exists());
                                log.info("Participants snapshot children count: " + participantsSnapshot.getChildrenCount());
                                
                                for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                                    log.info("Found participant: " + participant.getKey() + " = " + participant.getValue());
                                    participants.put(participant.getKey(), true);
                                }
                                
                                // Lấy thông tin customer (không phải admin)
                                String customerId = participants.keySet().stream()
                                        .filter(id -> !id.equals("admin"))
                                        .findFirst()
                                        .orElse(null);
                                
                                log.info("Room " + roomId + " has participants: " + participants.keySet() + ", customerId: " + customerId);
                                
                                if (customerId != null) {
                                    // Sử dụng cache thay vì query database mỗi lần
                                    User customer = getCachedUser(customerId);
                                    
                                    String lastMessage = roomSnapshot.child("lastMessage").getValue(String.class);
                                    Long lastMessageTime = roomSnapshot.child("lastMessageTime").getValue(Long.class);
                                    
                                    // Tạo customer name
                                    String customerName;
                                    if (customer != null) {
                                        customerName = customer.getUsername();
                                    } else {
                                        // Fallback: sử dụng customerId nếu không tìm thấy user
                                        customerName = "Customer " + customerId;
                                    }
                                    
                                    ChatRoomDTO roomDTO = ChatRoomDTO.builder()
                                            .roomId(roomId)
                                            .participants(participants)
                                            .lastMessage(lastMessage != null ? lastMessage : "No messages yet")
                                            .lastMessageTime(lastMessageTime != null ? 
                                                LocalDateTime.ofEpochSecond(lastMessageTime / 1000, 0, ZoneOffset.UTC) : 
                                                LocalDateTime.now())
                                            .customerName(customerName)
                                            .customerAvatar(null) // TODO: Add avatar field to User entity
                                            .isOnline(false) // TODO: Implement online status
                                            .unreadCount(0) // TODO: Implement unread count
                                            .build();
                                    
                                    rooms.add(roomDTO);
                                    log.info("Added room: " + roomId + " for customer: " + customerName);
                                } else {
                                    log.warn("No customer ID found for room: " + roomId + ", participants: " + participants.keySet());
                                }
                            } catch (Exception e) {
                                log.error("Error processing room: " + e.getMessage());
                            }
                        }
                        
                        // Sắp xếp theo thời gian tin nhắn cuối
                        rooms.sort((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()));
                        
                        log.info("Successfully retrieved " + rooms.size() + " chat rooms");
                    } else {
                        log.info("No chat rooms found in Firebase");
                    }
                    
                    future.complete(rooms);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    log.error("Error getting chat rooms: " + error.getMessage());
                    future.completeExceptionally(new RuntimeException("Failed to get chat rooms"));
                }
            });
            
            // Timeout sau 10 giây thay vì default timeout
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

    
    
    
    public void clearAllChatRooms() {
        log.info("Clearing all chat rooms...");
        try {
            databaseReference.child("ChatRooms").removeValueAsync();
            log.info("All chat rooms cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing chat rooms: " + e.getMessage(), e);
        }
    }

        public List<MessageDTO> getMessagesForRoom(String roomId) {
            log.info("Getting messages for room: " + roomId);
        
        CompletableFuture<List<MessageDTO>> future = new CompletableFuture<>();
        
        log.info("Fetching messages from Firebase for room: " + roomId);
        databaseReference.child("ChatRooms").child(roomId).child("messages")
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<MessageDTO> messageList = new ArrayList<>();
                        
                        log.info("Firebase response received. Messages count: " + snapshot.getChildrenCount());
                        
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
                            
                            log.info("Processing message: " + messageId + " from " + senderName + ": " + message);
                            
                            MessageDTO messageDTO = MessageDTO.builder()
                                    .messageId(messageId)
                                    .senderId(senderId)
                                    .senderName(senderName)
                                    .message(message)
                                    .timestamp(timestamp != null ? LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC) : LocalDateTime.now())
                                    .attachmentUrl(attachmentUrl)
                                    .attachmentType(attachmentType)
                                    .replyToMessageId(replyToMessageId)
                                    .isRead(isRead != null ? isRead : false)
                                    .roomId(roomId)
                                    .build();
                            
                            messageList.add(messageDTO);
                        }
                        
                        log.info("Processed " + messageList.size() + " messages from Firebase");
                        future.complete(messageList);
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        log.error("Error getting messages from Firebase: " + error.getMessage());
                        future.completeExceptionally(new RuntimeException("Failed to get messages from Firebase"));
                    }
                });
        
        try {
            log.info("Waiting for Firebase response...");
            List<MessageDTO> result = future.get();
            log.info("Successfully retrieved " + result.size() + " messages from Firebase");
            return result;
        } catch (Exception e) {
            log.error("Error getting messages from Firebase: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get messages from Firebase", e);
        }
    }

    public String getOrCreateChatRoom(String customerId) {
        log.info("Getting/creating chat room for customer: " + customerId);
        
        // Lấy thông tin customer để tạo room ID
        User customer = userRepository.findById(Integer.parseInt(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        String roomId = "chatRoom_" + customer.getUsername() + "_" + customerId;
        log.info("Room ID: " + roomId);

        // Kiểm tra xem room đã tồn tại chưa
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        log.info("Checking if room exists in Firebase...");
        databaseReference.child("ChatRooms").child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                log.info("Firebase response received. Room exists: " + snapshot.exists());
                if (!snapshot.exists()) {
                    log.info("Creating new room...");
                    // Tạo room mới với participants structure
                    Map<String, Object> roomData = new HashMap<>();
                    
                    // Tạo participants map
                    Map<String, Boolean> participants = new HashMap<>();
                    participants.put("admin", true);
                    participants.put(customerId, true);
                    roomData.put("participants", participants);
                    
                    roomData.put("createdAt", System.currentTimeMillis());
                    roomData.put("lastMessage", "Chat started");
                    roomData.put("lastMessageTime", System.currentTimeMillis());

                    // Sử dụng setValueAsync để lưu data
                    databaseReference.child("ChatRooms").child(roomId).setValueAsync(roomData);
                    log.info("New room created successfully with participants: " + participants.keySet());
                } else {
                    log.info("Room already exists");
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
            log.info("Waiting for Firebase response...");
            // Reduce timeout to 5 seconds for faster response
            future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            log.info("Chat room operation completed successfully");
            return roomId;
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Timeout waiting for Firebase response: " + e.getMessage());
            // Return room ID anyway to prevent hanging
            log.info("Returning room ID despite timeout: " + roomId);
            return roomId;
        } catch (Exception e) {
            log.error("Error getting/creating chat room: " + e.getMessage(), e);
            // Return room ID anyway to prevent hanging
            log.info("Returning room ID despite error: " + roomId);
            return roomId;
        }
    }

    private void sendNotificationToOtherParticipants(String roomId, String senderId, String senderName, String message) {
        // Lấy danh sách participants
        databaseReference.child("ChatRooms").child(roomId).child("participants")
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
        databaseReference.child("ChatRooms").child(roomId).child("messages").child(messageId)
                .child("isRead").setValueAsync(true);
    }

    public void markAllMessagesAsRead(String roomId, String userId) {
        databaseReference.child("ChatRooms").child(roomId).child("messages")
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
