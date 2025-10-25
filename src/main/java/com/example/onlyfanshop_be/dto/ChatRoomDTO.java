package com.example.onlyfanshop_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private String roomId;
    private Map<String, Boolean> participants;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String customerName;
    private String customerAvatar;
    private boolean isOnline;
    private int unreadCount;
}

