package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.entity.Notification;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.NotificationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 1️⃣ Tạo thông báo
    @PostMapping("/create")
    public ResponseEntity<Notification> createNotification(@RequestParam Integer userID, @RequestParam String message) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(notificationRepository.save(notification));
    }

    // 2️⃣ Lấy danh sách thông báo theo user
    @GetMapping("/user/{userID}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Integer userID) {
        return ResponseEntity.ok(notificationRepository.findByUser_UserIDOrderByCreatedAtDesc(userID));
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
}

