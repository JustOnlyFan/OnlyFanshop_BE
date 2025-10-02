package com.example.onlyfanshop_be.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationID;

    private String message;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "userID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;
}
