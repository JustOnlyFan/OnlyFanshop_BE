package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.ReviewStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews",
    indexes = {@Index(name = "idx_reviews_product_id", columnList = "product_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User user;

    @Column(name = "rating", nullable = false, columnDefinition = "TINYINT")
    private Byte rating; // 1-5

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "images_json", columnDefinition = "JSON")
    private String imagesJson; // Store as JSON string

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, 
        columnDefinition = "ENUM('pending','approved','rejected') DEFAULT 'pending'")
    @Builder.Default
    private ReviewStatus status = ReviewStatus.pending;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "approved_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime approvedAt;
}

