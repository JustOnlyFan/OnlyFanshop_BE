package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.NotificationDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Notification;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.NotificationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

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
                dto.setUsername(notification.getUser().getUsername());
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
        // 1. Lưu vào MySQL
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

        // 2. Gửi lên Firebase Realtime Database
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(String.valueOf(user.get().getId()));

        Map<String, Object> data = new HashMap<>();
        data.put("notificationID", noti.getNotificationID());
        data.put("message", message);
        data.put("isRead", false);
        data.put("createdAt", noti.getCreatedAt().toString());

        ref.child(String.valueOf(noti.getNotificationID())).setValueAsync(data);
    }
}
