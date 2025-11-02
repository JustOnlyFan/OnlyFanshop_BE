package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Order;
import com.example.onlyfanshop_be.enums.OrderStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status")
    List<Order> findOrdersByOrderStatus(@Param("status") OrderStatus status, Sort sort);
    
    @Query("SELECT o FROM Order o WHERE o.user.userID = :userId AND o.orderStatus = :status")
    List<Order> findOrdersByUser_UserIDAndOrderStatus(@Param("userId") int userId, @Param("status") OrderStatus status, Sort sort);
    
    List<Order> findOrdersByUser_UserID(int userId, Sort sort);
    
    // Keep old methods for compatibility (converting String to OrderStatus)
    // Note: JPQL doesn't support CAST with enum, so we use STR() function
    @Query("SELECT o FROM Order o WHERE STR(o.orderStatus) = :status")
    List<Order> findOrdersByOrderStatusString(@Param("status") String status, Sort sort);
    
    @Query("SELECT o FROM Order o WHERE o.user.userID = :userId AND STR(o.orderStatus) = :status")
    List<Order> findOrdersByUser_UserIDAndOrderStatusString(@Param("userId") int userId, @Param("status") String status, Sort sort);
}

