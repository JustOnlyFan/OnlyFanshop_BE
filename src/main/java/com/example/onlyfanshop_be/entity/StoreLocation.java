package com.example.onlyfanshop_be.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "StoreLocations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer locationID;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 255)
    private String address;
}

