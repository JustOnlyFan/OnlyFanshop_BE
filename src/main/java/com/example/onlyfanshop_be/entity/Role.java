package com.example.onlyfanshop_be.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roleID;

    @Column(nullable = false, unique = true, length = 50)
    private String roleName;

    @OneToMany(mappedBy = "role")
    private List<User> users;
}

