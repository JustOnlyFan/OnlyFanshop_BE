package com.example.onlyfanshop_be.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

    public void sendMessageToUser(String userToken, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setToken(userToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(AndroidNotification.builder()
                                    .setIcon("ic_notification")
                                    .setColor("#FF6B6B")
                                    .build())
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message: " + response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending message: " + e.getMessage());
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    public void sendChatNotification(String userToken, String senderName, String message, String roomId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "chat");
        data.put("roomId", roomId);
        data.put("senderName", senderName);
        
        sendMessageToUser(userToken, "Tin nhắn mới từ " + senderName, message, data);
    }

    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message to topic: " + response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending message to topic: " + e.getMessage());
            throw new RuntimeException("Failed to send notification to topic", e);
        }
    }
}

