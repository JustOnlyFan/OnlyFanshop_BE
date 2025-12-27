package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.NotificationDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Notification;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.NotificationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationRepository notificationRepository;

    // 2️⃣ Lấy danh sách thông báo theo user
    @GetMapping("/user/{userID}")
    public ApiResponse<List<NotificationDTO>> getUserNotifications(@PathVariable Integer userID) {
        return notificationService.getNotifications(userID);
    }

    // 3️⃣ Đánh dấu là đã đọc
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo!"));
        n.setIsRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok("Đã đánh dấu là đã đọc");
    }
    @GetMapping("/user/{userID}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Integer userID) {
        return ResponseEntity.ok(notificationRepository.countByUserIdAndIsReadFalse(userID != null ? userID.longValue() : null));
    }

}

