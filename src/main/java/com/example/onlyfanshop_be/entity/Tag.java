package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing product tags for marketing purposes.
 * Tags can be used to highlight products as new, bestseller, on-sale, premium, imported, or authentic.
 */
@Entity
@Table(name = "tags",
    indexes = {
        @Index(name = "idx_tags_code", columnList = "code")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT UNSIGNED")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    /**
     * Unique code for the tag (e.g., NEW, BESTSELLER, SALE, PREMIUM, IMPORTED, AUTHENTIC).
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Display name for the tag shown to users.
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * CSS color for badge display (e.g., #FF0000, red, rgb(255,0,0)).
     */
    @Column(name = "badge_color", length = 20)
    private String badgeColor;

    /**
     * Order in which tags should be displayed.
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tag")
    @JsonIgnore
    private List<ProductTag> productTags;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
