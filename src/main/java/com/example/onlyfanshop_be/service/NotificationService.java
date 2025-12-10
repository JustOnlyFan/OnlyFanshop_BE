package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.NotificationDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Notification;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.NotificationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public ApiResponse<List<NotificationDTO>> getNotifications(int userId) {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc((long) userId);
        if(list.isEmpty()){
            throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        List<NotificationDTO> listDTO = new ArrayList<>();
        for(Notification notification : list){
            NotificationDTO dto = new NotificationDTO();
            dto.setNotificationID(notification.getNotificationID());
            if (notification.getUser() != null) {
                dto.setFullName(notification.getUser().getFullname());
                dto.setUserID(notification.getUser().getId().intValue());
            }
            dto.setCreatedAt(notification.getCreatedAt());
            dto.setMessage(notification.getMessage());
            dto.setIsRead(notification.getIsRead());

            listDTO.add(dto);
        }
        return ApiResponse.<List<NotificationDTO>>builder().statusCode(200).data(listDTO).build();
    }
    
    public void sendNotification(int userId, String message) {
        // Lưu vào MySQL
        Optional<User> user = userRepository.findById((long) userId);
        if(user.isEmpty()){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        Notification noti = Notification.builder()
                .userId(user.get().getId())
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(noti);

        // Gửi qua WebSocket để real-time
        try {
            NotificationDTO dto = new NotificationDTO();
            dto.setNotificationID(noti.getNotificationID());
            dto.setFullName(user.get().getFullname());
            dto.setUserID(user.get().getId().intValue());
            dto.setCreatedAt(noti.getCreatedAt());
            dto.setMessage(message);
            dto.setIsRead(false);
            
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);
            log.info("Notification sent to user {} via WebSocket", userId);
        } catch (Exception e) {
            log.error("Error sending notification via WebSocket: " + e.getMessage(), e);
        }
    }
}
