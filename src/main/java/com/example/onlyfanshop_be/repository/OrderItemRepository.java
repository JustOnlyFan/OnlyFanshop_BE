package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId ORDER BY oi.id ASC")
    List<OrderItem> findOrderItemsByOrderId(@Param("orderId") Long orderId);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default List<OrderItem> findByOrder_OrderID(Integer orderID) {
        return findByOrderId(orderID != null ? orderID.longValue() : null);
    }
    
    @Deprecated
    default List<OrderItem> findOrderItemsByOrderID(Integer orderID) {
        return findOrderItemsByOrderId(orderID != null ? orderID.longValue() : null);
    }
}
