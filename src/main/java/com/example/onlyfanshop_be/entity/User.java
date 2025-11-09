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

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

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

    // Legacy field for backward compatibility (username)
    @Transient
    public String getUsername() {
        return fullName; // Use fullName as username for backward compatibility
    }
    
    @Transient
    public void setUsername(String username) {
        this.fullName = username;
    }

    // Legacy field for backward compatibility (fcmToken)
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;
    
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
        // This is a legacy method - address should be managed through UserAddress entity
        // For backward compatibility, we do nothing here
        // The actual address update should be done through UserAddressService
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

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Notification> notifications;

    @OneToMany(mappedBy = "sender")
    @JsonIgnore
    private List<ChatMessage> chatMessages;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Token> tokens;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<ProductReview> productReviews;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Wishlist> wishlists;
}
