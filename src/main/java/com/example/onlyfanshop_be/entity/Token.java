package com.example.onlyfanshop_be.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID")
    private User user;
}
