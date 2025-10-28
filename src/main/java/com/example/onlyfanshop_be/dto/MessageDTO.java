package com.example.onlyfanshop_be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    
    // Keep both for backward compatibility: epochMillis preferred by mobile
    private Long epochMillis;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String attachmentUrl;
    private String attachmentType;
    private String replyToMessageId;
    private boolean isRead;
    private String roomId;
}
