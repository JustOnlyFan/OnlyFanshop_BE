package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    private boolean expired;   // true nếu token hết hạn
    private boolean revoked;   // true nếu user logout

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType type;     // ACCESS hoặc REFRESH

    @Column(nullable = true)
    private Instant expiresAt;  // thời điểm hết hạn (nullable để migrate an toàn)

    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
