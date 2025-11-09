package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default List<Notification> findByUser_UserIDOrderByCreatedAtDesc(Integer userID) {
        return findByUserIdOrderByCreatedAtDesc(userID != null ? userID.longValue() : null);
    }
    
    @Deprecated
    default long countByUser_UserIDAndIsReadFalse(Integer userID) {
        return countByUserIdAndIsReadFalse(userID != null ? userID.longValue() : null);
    }
}
