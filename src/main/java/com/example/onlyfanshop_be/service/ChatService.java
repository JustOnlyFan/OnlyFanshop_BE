package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ChatRoomDTO;
import com.example.onlyfanshop_be.dto.MessageDTO;
import com.example.onlyfanshop_be.dto.request.CreateChatRoomFromProductRequest;
import com.example.onlyfanshop_be.dto.request.CreateChatRoomRequest;
import com.example.onlyfanshop_be.dto.request.SendMessageRequest;
import com.example.onlyfanshop_be.entity.ChatMessage;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.ChatMessageRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.RoleRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Tạo chat room mới (thực chất là gửi tin nhắn đầu tiên)
     */
    @Transactional
    public String createChatRoom(CreateChatRoomRequest request, String adminId) {
        User customer = userRepository.findById(Long.parseLong(request.getCustomerId()))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        User admin = userRepository.findById(Long.parseLong(adminId))
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        String roomId = "chatRoom_" + customer.getFullname() + "_" + request.getCustomerId();

        // Gửi tin nhắn đầu tiên nếu có
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            sendMessage(SendMessageRequest.builder()
                    .roomId(roomId)
                    .message(request.getInitialMessage())
                    .build(), request.getCustomerId());
        }

        return roomId;
    }

    /**
     * Gửi tin nhắn và lưu vào MySQL
     */
    @Transactional
    public void sendMessage(SendMessageRequest request, String senderId) {
        User sender = userRepository.findById(Long.parseLong(senderId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Extract receiver ID from roomId
        String receiverId = extractReceiverIdFromRoomId(request.getRoomId(), senderId);
        User receiver = userRepository.findById(Long.parseLong(receiverId))
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        
        // Lưu tin nhắn vào MySQL
        ChatMessage chatMessage = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .message(request.getMessage())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentType(request.getAttachmentType())
                .replyToMessageId(request.getReplyToMessageId())
                .sentAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        chatMessage = chatMessageRepository.save(chatMessage);
        
        // Gửi qua WebSocket để real-time
        MessageDTO messageDTO = convertToMessageDTO(chatMessage, request.getRoomId());
        messagingTemplate.convertAndSend("/topic/chat/" + request.getRoomId(), messageDTO);
        
        log.info("Message sent from {} to {} in room {}", senderId, receiverId, request.getRoomId());
    }

    /**
     * Gửi tin nhắn qua WebSocket (được gọi từ WebSocketChatController)
     */
    @Transactional
    public void sendMessageViaWebSocket(SendMessageRequest request, String senderId) {
        // Chỉ cần gọi sendMessage, WebSocket đã được xử lý trong đó
        sendMessage(request, senderId);
    }

    /**
     * Lấy danh sách chat rooms cho admin
     */
    public List<ChatRoomDTO> getChatRoomsForAdmin() {
        try {
            log.info("Getting chat rooms for admin...");
            
            // Lấy tất cả users có role customer
            Role customerRole = roleRepository.findByName("customer")
                    .orElseThrow(() -> new RuntimeException("Customer role not found"));
            
            List<User> customers = userRepository.findByRoleId(customerRole.getId());
            
            // Lấy admin user
            Role adminRole = roleRepository.findByName("admin")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            List<User> admins = userRepository.findByRoleId(adminRole.getId());
            
            if (admins.isEmpty()) {
                log.warn("No admin found");
                return new ArrayList<>();
            }
            
            User admin = admins.get(0);
            
            List<ChatRoomDTO> rooms = new ArrayList<>();
            
            for (User customer : customers) {
                // Lấy tin nhắn mới nhất giữa admin và customer
                Optional<ChatMessage> latestMessage = chatMessageRepository
                        .findLatestMessageBetweenUsers(admin.getId(), customer.getId());
                
                if (latestMessage.isPresent()) {
                    ChatMessage msg = latestMessage.get();
                    String roomId = "chatRoom_" + customer.getFullname() + "_" + customer.getId();
                    
                    Map<String, Boolean> participants = new HashMap<>();
                    participants.put("admin", true);
                    participants.put(customer.getId().toString(), true);
                    
                    ChatRoomDTO roomDTO = ChatRoomDTO.builder()
                            .roomId(roomId)
                            .participants(participants)
                            .lastMessage(msg.getMessage())
                            .lastMessageTime(msg.getSentAt())
                            .customerName(customer.getFullname())
                            .customerAvatar(null)
                            .isOnline(false)
                            .unreadCount(0)
                            .build();
                    
                    rooms.add(roomDTO);
                }
            }
            
            // Sắp xếp theo thời gian tin nhắn mới nhất
            rooms.sort((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()));
            
            log.info("Returning " + rooms.size() + " chat rooms");
            return rooms;
            
        } catch (Exception e) {
            log.error("Error getting chat rooms: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Xóa tất cả chat rooms (for testing)
     */
    @Transactional
    public void clearAllChatRooms() {
        log.info("Clearing all chat rooms...");
        try {
            chatMessageRepository.deleteAll();
            log.info("All chat rooms cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing chat rooms: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy tin nhắn trong một room
     */
    public List<MessageDTO> getMessagesForRoom(String roomId) {
        try {
            // Extract user IDs from roomId
            String[] parts = roomId.split("_");
            if (parts.length < 3) {
                throw new RuntimeException("Invalid room ID format");
            }
            
            String customerId = parts[2];
            
            // Lấy admin
            Role adminRole = roleRepository.findByName("admin")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            List<User> admins = userRepository.findByRoleId(adminRole.getId());
            
            if (admins.isEmpty()) {
                throw new RuntimeException("No admin found");
            }
            
            User admin = admins.get(0);
            
            // Lấy tin nhắn giữa admin và customer
            Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "sentAt"));
            Page<ChatMessage> messagesPage = chatMessageRepository
                    .findMessagesBetweenUsers(admin.getId(), Long.parseLong(customerId), pageable);
            
            return messagesPage.getContent().stream()
                    .map(msg -> convertToMessageDTO(msg, roomId))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting messages: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get messages", e);
        }
    }

    /**
     * Lấy hoặc tạo chat room cho customer
     */
    @Transactional
    public String getOrCreateChatRoom(String customerId) {
        User customer = userRepository.findById(Long.parseLong(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        String roomId = "chatRoom_" + customer.getFullname() + "_" + customerId;
        
        log.info("Getting or creating chat room for customer: " + customerId + ", roomId: " + roomId);
        
        return roomId;
    }

    /**
     * Tạo chat room từ product
     */
    @Transactional
    public String createChatRoomFromProduct(String customerId, CreateChatRoomFromProductRequest request) {
        User customer = userRepository.findById(Long.parseLong(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        Product product = productRepository.findById(request.getProductId().intValue())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        String roomId = "chatRoom_" + customer.getFullname() + "_" + customerId;
        
        log.info("Creating chat room from product for customer: " + customerId + ", product: " + product.getId());

        // Gửi tin nhắn đầu tiên nếu có
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            sendMessage(SendMessageRequest.builder()
                    .roomId(roomId)
                    .message(request.getInitialMessage())
                    .build(), customerId);
        }

        return roomId;
    }

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    @Transactional
    public void markMessageAsRead(String roomId, String messageId) {
        // Implementation if needed
        log.info("Marking message as read: " + messageId);
    }

    /**
     * Đánh dấu tất cả tin nhắn đã đọc
     */
    @Transactional
    public void markAllMessagesAsRead(String roomId, String userId) {
        log.info("Marking all messages as read in room: " + roomId + " for user: " + userId);
    }

    /**
     * Lấy chat rooms cho staff
     */
    public List<ChatRoomDTO> getChatRoomsForStaff(String staffId) {
        // Similar to admin implementation
        return getChatRoomsForAdmin();
    }

    /**
     * Lấy chat rooms cho customer
     */
    public List<ChatRoomDTO> getChatRoomsForCustomer(String customerId) {
        try {
            log.info("Getting chat rooms for customer: " + customerId);
            
            User customer = userRepository.findById(Long.parseLong(customerId))
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            
            // Lấy admin
            Role adminRole = roleRepository.findByName("admin")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            List<User> admins = userRepository.findByRoleId(adminRole.getId());
            
            if (admins.isEmpty()) {
                return new ArrayList<>();
            }
            
            User admin = admins.get(0);
            
            // Lấy tin nhắn mới nhất
            Optional<ChatMessage> latestMessage = chatMessageRepository
                    .findLatestMessageBetweenUsers(admin.getId(), customer.getId());
            
            List<ChatRoomDTO> rooms = new ArrayList<>();
            
            if (latestMessage.isPresent()) {
                ChatMessage msg = latestMessage.get();
                String roomId = "chatRoom_" + customer.getFullname() + "_" + customer.getId();
                
                Map<String, Boolean> participants = new HashMap<>();
                participants.put("admin", true);
                participants.put(customer.getId().toString(), true);
                
                ChatRoomDTO roomDTO = ChatRoomDTO.builder()
                        .roomId(roomId)
                        .participants(participants)
                        .lastMessage(msg.getMessage())
                        .lastMessageTime(msg.getSentAt())
                        .customerName("Admin")
                        .customerAvatar(null)
                        .isOnline(false)
                        .unreadCount(0)
                        .build();
                
                rooms.add(roomDTO);
            }
            
            return rooms;
            
        } catch (Exception e) {
            log.error("Error getting chat rooms for customer: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Helper methods
    
    private String extractReceiverIdFromRoomId(String roomId, String senderId) {
        // Room ID format: chatRoom_username_userId
        String[] parts = roomId.split("_");
        if (parts.length >= 3) {
            String customerId = parts[2];
            
            // Nếu sender là customer, receiver là admin
            if (customerId.equals(senderId)) {
                Role adminRole = roleRepository.findByName("admin")
                        .orElseThrow(() -> new RuntimeException("Admin role not found"));
                List<User> admins = userRepository.findByRoleId(adminRole.getId());
                if (!admins.isEmpty()) {
                    return admins.get(0).getId().toString();
                }
            } else {
                // Nếu sender là admin, receiver là customer
                return customerId;
            }
        }
        throw new RuntimeException("Cannot extract receiver ID from room ID: " + roomId);
    }
    
    private MessageDTO convertToMessageDTO(ChatMessage msg, String roomId) {
        return MessageDTO.builder()
                .messageId(msg.getChatMessageID().toString())
                .senderId(msg.getSender().getId().toString())
                .senderName(msg.getSender().getFullname())
                .message(msg.getMessage())
                .timestamp(msg.getSentAt())
                .epochMillis(msg.getSentAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                .attachmentUrl(msg.getAttachmentUrl())
                .attachmentType(msg.getAttachmentType())
                .replyToMessageId(msg.getReplyToMessageId())
                .isRead(false)
                .roomId(roomId)
                .build();
    }
}
