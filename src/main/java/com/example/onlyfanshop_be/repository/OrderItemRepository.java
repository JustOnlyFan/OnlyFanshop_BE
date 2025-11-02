package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}
