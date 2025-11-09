package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Order;
import com.example.onlyfanshop_be.enums.OrderStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    List<Order> findOrdersByStatus(@Param("status") OrderStatus status, Sort sort);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status")
    List<Order> findOrdersByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status, Sort sort);
    
    List<Order> findByUserId(Long userId, Sort sort);
    
    Optional<Order> findByOrderCode(String orderCode);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default List<Order> findOrdersByOrderStatus(OrderStatus status, Sort sort) {
        return findOrdersByStatus(status, sort);
    }
    
    @Deprecated
    default List<Order> findOrdersByUser_UserIDAndOrderStatus(int userId, OrderStatus status, Sort sort) {
        return findOrdersByUserIdAndStatus((long) userId, status, sort);
    }
    
    @Deprecated
    default List<Order> findOrdersByUser_UserID(int userId, Sort sort) {
        return findByUserId((long) userId, sort);
    }
    
    @Deprecated
    default long countByUser_UserIDAndOrderStatus(int accountId, OrderStatus status) {
        return countByUserIdAndStatus((long) accountId, status);
    }
    
    long countByUserIdAndStatus(Long userId, OrderStatus status);
}

