package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.NotificationDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Notification;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    public ApiResponse<List<NotificationDTO>> getNotifications(int userId) {
        List<Notification> list = notificationRepository.findByUser_UserIDOrderByCreatedAtDesc(userId);
        if(list.isEmpty()){
            throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        List<NotificationDTO> listDTO = new ArrayList<>();
        for(Notification notification : list){
            NotificationDTO dto = new NotificationDTO();
            dto.setNotificationID(notification.getNotificationID());
            dto.setUsername(notification.getUser().getUsername());
            dto.setCreatedAt(notification.getCreatedAt());
            dto.setMessage(notification.getMessage());
            dto.setUserID(notification.getUser().getUserID());
            dto.setIsRead(notification.getIsRead());

            listDTO.add(dto);
        }
        return ApiResponse.<List<NotificationDTO>>builder().statusCode(200).data(listDTO).build();
    }
}
