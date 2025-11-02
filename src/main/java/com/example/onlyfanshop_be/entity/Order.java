package com.example.onlyfanshop_be.entity;
import com.example.onlyfanshop_be.enums.DeliveryType;
import com.example.onlyfanshop_be.enums.OrderStatus;
import com.example.onlyfanshop_be.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod  paymentMethod;

    @Column(nullable = false, length = 255)
    private String billingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus  orderStatus;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType deliveryType;

    @ManyToOne
    @JoinColumn(name = "storeID", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private StoreLocation pickupStore;

    @Column(nullable = true, length = 255)
    private String shippingAddress;

    @Column(nullable = true, length = 20)
    private String recipientPhoneNumber;  // Số điện thoại người nhận

    @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 0.0")
    @Builder.Default
    private Double totalPrice = 0.0;

    @ManyToOne
    @JoinColumn(name = "userID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    private List<Payment> payments;
}