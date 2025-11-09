package com.example.onlyfanshop_be.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer chatMessageID;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String attachmentUrl;

    @Column(length = 100)
    private String attachmentType;


    @Column(length = 50)
    private String replyToMessageId;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", columnDefinition = "BIGINT UNSIGNED")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", columnDefinition = "BIGINT UNSIGNED")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User receiver;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


