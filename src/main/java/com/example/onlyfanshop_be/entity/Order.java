package com.example.onlyfanshop_be.entity;
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

    @Column(nullable = false, length = 50)
    private String paymentMethod;

    @Column(nullable = false, length = 255)
    private String billingAddress;

    @Column(nullable = false, length = 50)
    private String orderStatus;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @ManyToOne
    @JoinColumn(name = "cartID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "userID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    private List<Payment> payments;
}
