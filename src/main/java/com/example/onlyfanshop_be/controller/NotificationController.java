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
//     1️⃣ Tạo thông báo
//    @PostMapping("/create")
//    public ResponseEntity<NotificationDTO> createNotification(@RequestParam Integer userID, @RequestParam String message) {
//        User user = userRepository.findById(userID)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
//
//        NotificationDTO notification = Notification.builder()
//                .user(user)
//                .message(message)
//                .isRead(false)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        return ResponseEntity.ok(notificationRepository.save(notification));
//    }

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
        return ResponseEntity.ok(notificationRepository.countByUser_UserIDAndIsReadFalse(userID));
    }

}

