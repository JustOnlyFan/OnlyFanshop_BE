package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "role_id", nullable = false, columnDefinition = "TINYINT UNSIGNED DEFAULT 1")
    private Byte roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    @JsonIgnore
    private Role role;

    @Column(name = "fullname", nullable = false, length = 100)
    private String fullname;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('active','inactive','banned') DEFAULT 'active'")
    @Builder.Default
    private UserStatus status = UserStatus.active;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime lastLoginAt;

    @Column(name = "store_location_id", columnDefinition = "INT UNSIGNED")
    private Integer storeLocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_location_id", insertable = false, updatable = false)
    @JsonIgnore
    private StoreLocation storeLocation;
    
    // Legacy methods for backward compatibility
    @Transient
    public Integer getUserID() {
        return id != null ? id.intValue() : null;
    }
    
    @Transient
    public String getPhoneNumber() {
        return phone;
    }
    
    @Transient
    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
    }
    
    @Transient
    public String getAddress() {
        // Return first default address if available
        if (addresses != null && !addresses.isEmpty()) {
            return addresses.stream()
                    .filter(UserAddress::getIsDefault)
                    .map(addr -> addr.getAddressLine1() + 
                        (addr.getAddressLine2() != null ? ", " + addr.getAddressLine2() : "") +
                        (addr.getWard() != null ? ", " + addr.getWard() : "") +
                        (addr.getDistrict() != null ? ", " + addr.getDistrict() : "") +
                        (addr.getCity() != null ? ", " + addr.getCity() : ""))
                    .findFirst()
                    .orElse(addresses.get(0).getAddressLine1());
        }
        return null;
    }
    
    @Transient
    public void setAddress(String address) {
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserAddress> addresses;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Cart> carts;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Token> tokens;
}
