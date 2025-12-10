package com.example.onlyfanshop_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Integer notificationID;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Integer userID;
    private String fullName;
}
