package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "Tokens")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID")
    private User user;
}
