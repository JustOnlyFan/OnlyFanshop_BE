package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUser_UserIDOrderByCreatedAtDesc(Integer userID);
    long countByUser_UserIDAndIsReadFalse(Integer userID);
//
//    @Query("SELECT n FROM Notification n WHERE n.user.userID = :userID ORDER BY n.createdAt DESC")
//    List<Notification> findByUser_UserIDOrderByCreatedAtDesc(@Param("userID") Integer userID);
}
