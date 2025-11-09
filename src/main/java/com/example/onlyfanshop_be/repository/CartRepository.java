package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
    List<Cart> findAllByUserId(Long userId);
    Optional<Cart> findBySessionId(String sessionId);
    List<Cart> findAllBySessionId(String sessionId);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default List<Cart> findByUser_username(String username) {
        // Note: Username check removed, use userId or email instead
        return List.of();
    }
    
    @Deprecated
    default List<Cart> findByStatusAndUser_username(String status, String username) {
        // Note: Status field removed from Cart, use userId instead
        return List.of();
    }
    
    @Deprecated
    default Optional<Cart> findByUser_UserID(Integer userID) {
        return findByUserId(userID != null ? userID.longValue() : null);
    }
    
    @Deprecated
    default Optional<Cart> findByUser_UserIDAndStatus(Integer userUserID, String status) {
        // Note: Status field removed from Cart
        return findByUserId(userUserID != null ? userUserID.longValue() : null);
    }
}

