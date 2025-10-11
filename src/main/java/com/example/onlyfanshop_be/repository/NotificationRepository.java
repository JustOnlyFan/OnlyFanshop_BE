package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUser_UserIDOrderByCreatedAtDesc(Integer userID);
    long countByUser_UserIDAndIsReadFalse(Integer userID);
}

