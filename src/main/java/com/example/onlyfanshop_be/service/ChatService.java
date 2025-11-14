package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ChatRoomDTO;
import com.example.onlyfanshop_be.dto.MessageDTO;
import com.example.onlyfanshop_be.dto.request.CreateChatRoomFromProductRequest;
import com.example.onlyfanshop_be.dto.request.CreateChatRoomRequest;
import com.example.onlyfanshop_be.dto.request.SendMessageRequest;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.RoleRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.google.firebase.database.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final DatabaseReference databaseReference;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;

    public String createChatRoom(CreateChatRoomRequest request, String adminId) {
        // Lấy thông tin customer để tạo room ID
        User customer = userRepository.findById(Long.parseLong(request.getCustomerId()))
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
        User sender = userRepository.findById(Long.parseLong(senderId))
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
        
        // Lưu tin nhắn vào Firebase (for persistence)
        databaseReference.child("Messages").child(request.getRoomId()).child(messageId)
                .setValueAsync(messageData);
        
        // Cập nhật lastMessage và lastMessageTime
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastMessage", request.getMessage());
        updateData.put("lastMessageTime", System.currentTimeMillis());
        
        databaseReference.child("Conversations").child(request.getRoomId()).updateChildrenAsync(updateData);
        
        // Note: Real-time delivery is handled by WebSocket, Firebase is for persistence
        // Gửi FCM notification (optional, for offline users)
        sendNotificationToOtherParticipants(request.getRoomId(), senderId, sender.getUsername(), request.getMessage());
    }
    
    /**
     * Send message via WebSocket (called from WebSocketChatController)
     * This method only saves to Firebase, WebSocket handles real-time delivery
     */
    public void sendMessageViaWebSocket(SendMessageRequest request, String senderId) {
        // Just save to Firebase for persistence
        // WebSocket will handle real-time delivery
        sendMessage(request, senderId);
    }

        public List<ChatRoomDTO> getChatRoomsForAdmin() {
        try {
            log.info("Getting chat rooms for admin...");
            CompletableFuture<List<ChatRoomDTO>> future = new CompletableFuture<>();
            
            databaseReference.child("Conversations").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<ChatRoomDTO> rooms = new ArrayList<>();
                    
                    log.info("Firebase snapshot exists: " + snapshot.exists() + ", children count: " + snapshot.getChildrenCount());
                    
                    if (snapshot.exists()) {
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            log.info("Processing room: " + roomSnapshot.getKey());
                            try {
                                String roomId = roomSnapshot.getKey();
                                
                                // Lấy thông tin participants
                                Map<String, Boolean> participants = new HashMap<>();
                                DataSnapshot participantsSnapshot = roomSnapshot.child("participants");
                                
                                log.info("Room " + roomId + " participants snapshot exists: " + participantsSnapshot.exists());
                                
                                if (participantsSnapshot.exists()) {
                                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                                        String participantKey = participant.getKey();
                                        participants.put(participantKey, true);
                                        log.info("Found participant: " + participantKey);
                                    }
                                } else {
                                    log.warn("No participants found in room: " + roomId);
                                }
                                
                                log.info("All participants in room " + roomId + ": " + participants.keySet());
                                
                                // Lấy thông tin customer (không phải admin)
                                // ✅ Fix: Ưu tiên customer ID (số), nếu không có thì lấy bất kỳ ID nào không phải admin
                                String customerId = null;
                                
                                // Bước 1: Tìm customer ID là số (ưu tiên)
                                for (String id : participants.keySet()) {
                                    if (!id.equals("admin") && id.matches("\\d+")) {
                                        customerId = id;
                                        log.info("Found numeric customer ID: " + customerId);
                                        break;
                                    }
                                }
                                
                                // Bước 2: Nếu không có customer ID số, thử extract từ roomId
                                if (customerId == null) {
                                    // Room ID format: chatRoom_username_userId
                                    if (roomId != null && roomId.startsWith("chatRoom_")) {
                                        String[] parts = roomId.split("_");
                                        if (parts.length >= 3) {
                                            String extractedId = parts[2];
                                            if (extractedId.matches("\\d+")) {
                                                customerId = extractedId;
                                                log.info("Extracted customer ID from roomId: " + customerId);
                                                // ✅ Thêm customer ID vào participants nếu chưa có
                                                databaseReference.child("Conversations").child(roomId)
                                                    .child("participants").child(customerId).setValueAsync(true);
                                            }
                                        }
                                    }
                                }
                                
                                // Bước 3: Nếu vẫn không có, lấy bất kỳ ID nào không phải admin (fallback)
                                if (customerId == null) {
                                    customerId = participants.keySet().stream()
                                            .filter(id -> !id.equals("admin"))
                                            .findFirst()
                                            .orElse(null);
                                    if (customerId != null) {
                                        log.warn("Using non-numeric participant ID as customer ID: " + customerId + " (may be Firebase UID)");
                                    }
                                }
                                
                                log.info("Final customer ID for room " + roomId + ": " + customerId);
                                
                                if (customerId != null) {
                                    log.info("Found customer ID: " + customerId + " in room: " + roomId);
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
                                    
                                    log.info("Creating ChatRoomDTO for room: " + roomId + ", customer: " + customerName);
                                    
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
                                } else {
                                    log.warn("No customer ID found in room: " + roomId + ", participants: " + participants.keySet());
                                }
                            } catch (Exception e) {
                                log.error("Error processing room: " + e.getMessage());
                            }
                        }
                        
                        rooms.sort((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()));
                    } else {
                        log.warn("No conversations found in Firebase");
                    }
                    
                    log.info("Returning " + rooms.size() + " chat rooms");
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
            User user = userRepository.findById(Long.parseLong(userId)).orElse(null);
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
        User customer = userRepository.findById(Long.parseLong(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Get a staff member (preferably one with a store assigned)
        User staff = getAvailableStaff();
        if (staff == null) {
            throw new RuntimeException("No staff available");
        }
        
        String roomId = "chatRoom_" + customer.getUsername() + "_" + customerId + "_staff_" + staff.getId();

        log.info("Getting or creating chat room for customer: " + customerId + " with staff: " + staff.getId() + ", roomId: " + roomId);

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        databaseReference.child("Conversations").child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    log.info("Room does not exist, creating new room: " + roomId);
                    Map<String, Object> roomData = new HashMap<>();
                    Map<String, Boolean> participants = new HashMap<>();
                    participants.put(staff.getId().toString(), true); // Staff ID
                    participants.put(customerId, true); // Customer ID
                    roomData.put("participants", participants);
                    roomData.put("staffId", staff.getId().toString());
                    roomData.put("staffName", staff.getUsername());
                    roomData.put("customerId", customerId);
                    roomData.put("customerName", customer.getUsername());
                    roomData.put("createdAt", System.currentTimeMillis());
                    roomData.put("lastMessage", "Chat started");
                    roomData.put("lastMessageTime", System.currentTimeMillis());
                    databaseReference.child("Conversations").child(roomId).setValueAsync(roomData);
                    log.info("Room created successfully: " + roomId);
                } else {
                    log.info("Room already exists: " + roomId);
                    // ✅ Đảm bảo customer ID có trong participants (nếu chưa có)
                    DataSnapshot participantsSnapshot = snapshot.child("participants");
                    boolean hasCustomerId = false;
                    if (participantsSnapshot.exists()) {
                        for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                            if (participant.getKey().equals(customerId)) {
                                hasCustomerId = true;
                                break;
                            }
                        }
                    }
                    if (!hasCustomerId) {
                        log.info("Adding customer ID to participants: " + customerId);
                        databaseReference.child("Conversations").child(roomId).child("participants").child(customerId).setValueAsync(true);
                    }
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

    public String createChatRoomFromProduct(String customerId, CreateChatRoomFromProductRequest request) {
        User customer = userRepository.findById(Long.parseLong(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        Product product = productRepository.findById(request.getProductId().intValue())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Get a staff member (preferably one with a store assigned)
        User staff = getAvailableStaff();
        if (staff == null) {
            throw new RuntimeException("No staff available");
        }
        
        String roomId = "chatRoom_" + customer.getUsername() + "_" + customerId + "_staff_" + staff.getId() + "_product_" + product.getId();

        log.info("Creating chat room from product for customer: " + customerId + " with staff: " + staff.getId() + ", product: " + product.getId());

        Map<String, Object> roomData = new HashMap<>();
        Map<String, Boolean> participants = new HashMap<>();
        participants.put(staff.getId().toString(), true);
        participants.put(customerId, true);
        roomData.put("participants", participants);
        roomData.put("staffId", staff.getId().toString());
        roomData.put("staffName", staff.getUsername());
        roomData.put("customerId", customerId);
        roomData.put("customerName", customer.getUsername());
        roomData.put("productId", product.getId().toString());
        roomData.put("productName", product.getName());
        roomData.put("productImage", product.getImageURL());
        roomData.put("createdAt", System.currentTimeMillis());
        roomData.put("lastMessage", request.getInitialMessage() != null ? request.getInitialMessage() : "Chat started about product");
        roomData.put("lastMessageTime", System.currentTimeMillis());

        databaseReference.child("Conversations").child(roomId).setValueAsync(roomData);

        // Send initial message if provided
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            sendMessage(SendMessageRequest.builder()
                    .roomId(roomId)
                    .message(request.getInitialMessage())
                    .build(), customerId);
        }

        return roomId;
    }

    private User getAvailableStaff() {
        Role staffRole = roleRepository.findByName("staff").orElse(null);
        if (staffRole == null) {
            return null;
        }
        
        List<User> staffList = userRepository.findByRoleId(staffRole.getId());
        if (staffList.isEmpty()) {
            return null;
        }
        
        // Prefer staff with store assigned, otherwise return first available
        List<User> staffWithStore = staffList.stream()
                .filter(s -> s.getStoreLocationId() != null)
                .collect(Collectors.toList());
        
        if (!staffWithStore.isEmpty()) {
            // Return random staff with store
            Collections.shuffle(staffWithStore);
            return staffWithStore.get(0);
        }
        
        // Return first available staff
        return staffList.get(0);
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
                                // ✅ Handle "admin" participant ID - skip notification for admin
                                if ("admin".equals(participantId)) {
                                    log.info("Skipping notification for admin participant");
                                    continue;
                                }
                                
                                // ✅ Only parse numeric participant IDs
                                // Note: FCM notifications removed for simplicity (family e-commerce)
                                try {
                                    Long userId = Long.parseLong(participantId);
                                    // Chat notifications disabled - not needed for family e-commerce
                                    log.debug("Chat message sent to user: " + userId);
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

    public List<ChatRoomDTO> getChatRoomsForStaff(String staffId) {
        try {
            log.info("Getting chat rooms for staff: " + staffId);
            CompletableFuture<List<ChatRoomDTO>> future = new CompletableFuture<>();
            
            databaseReference.child("Conversations").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<ChatRoomDTO> rooms = new ArrayList<>();
                    
                    if (snapshot.exists()) {
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            try {
                                String roomId = roomSnapshot.getKey();
                                DataSnapshot participantsSnapshot = roomSnapshot.child("participants");
                                
                                // Check if this staff is a participant
                                boolean isParticipant = false;
                                if (participantsSnapshot.exists()) {
                                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                                        if (participant.getKey().equals(staffId)) {
                                            isParticipant = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (isParticipant) {
                                    Map<String, Boolean> participants = new HashMap<>();
                                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                                        participants.put(participant.getKey(), true);
                                    }
                                    
                                    String customerName = roomSnapshot.child("customerName").getValue(String.class);
                                    if (customerName == null) {
                                        customerName = extractCustomerNameFromRoomId(roomId);
                                    }
                                    
                                    String lastMessage = roomSnapshot.child("lastMessage").getValue(String.class);
                                    Long lastMessageTime = roomSnapshot.child("lastMessageTime").getValue(Long.class);
                                    
                                    ChatRoomDTO roomDTO = ChatRoomDTO.builder()
                                            .roomId(roomId)
                                            .participants(participants)
                                            .lastMessage(lastMessage != null ? lastMessage : "No messages yet")
                                            .lastMessageTime(lastMessageTime != null ? 
                                                LocalDateTime.ofEpochSecond(lastMessageTime / 1000, 0, ZoneOffset.UTC) : 
                                                LocalDateTime.now())
                                            .customerName(customerName != null ? customerName : "Customer")
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
            log.error("Error getting chat rooms for staff: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<ChatRoomDTO> getChatRoomsForCustomer(String customerId) {
        try {
            log.info("Getting chat rooms for customer: " + customerId);
            CompletableFuture<List<ChatRoomDTO>> future = new CompletableFuture<>();
            
            databaseReference.child("Conversations").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<ChatRoomDTO> rooms = new ArrayList<>();
                    
                    if (snapshot.exists()) {
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            try {
                                String roomId = roomSnapshot.getKey();
                                DataSnapshot participantsSnapshot = roomSnapshot.child("participants");
                                
                                // Check if this customer is a participant
                                boolean isParticipant = false;
                                if (participantsSnapshot.exists()) {
                                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                                        if (participant.getKey().equals(customerId)) {
                                            isParticipant = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (isParticipant) {
                                    Map<String, Boolean> participants = new HashMap<>();
                                    for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                                        participants.put(participant.getKey(), true);
                                    }
                                    
                                    String staffName = roomSnapshot.child("staffName").getValue(String.class);
                                    if (staffName == null) {
                                        staffName = "Staff";
                                    }
                                    
                                    String lastMessage = roomSnapshot.child("lastMessage").getValue(String.class);
                                    Long lastMessageTime = roomSnapshot.child("lastMessageTime").getValue(Long.class);
                                    
                                    ChatRoomDTO roomDTO = ChatRoomDTO.builder()
                                            .roomId(roomId)
                                            .participants(participants)
                                            .lastMessage(lastMessage != null ? lastMessage : "No messages yet")
                                            .lastMessageTime(lastMessageTime != null ? 
                                                LocalDateTime.ofEpochSecond(lastMessageTime / 1000, 0, ZoneOffset.UTC) : 
                                                LocalDateTime.now())
                                            .customerName(staffName) // For customer view, show staff name
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
            log.error("Error getting chat rooms for customer: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
