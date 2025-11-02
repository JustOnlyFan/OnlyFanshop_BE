package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder_OrderID(Integer orderID);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderID = :orderID ORDER BY oi.orderItemID ASC")
    List<OrderItem> findOrderItemsByOrderID(@Param("orderID") Integer orderID);
}
