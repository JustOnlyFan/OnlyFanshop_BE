package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findOrdersByOrderStatus(String status, Sort sort);
    List<Order> findOrdersByUser_UserIDAndOrderStatus(int userId, String status, Sort sort);
    List<Order> findOrdersByUser_UserID(int userId, Sort sort);
}

