package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    /**
     * Lấy tin nhắn theo conversation với phân trang
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "(cm.sender.userID = :userId1 AND cm.receiver.userID = :userId2) OR " +
           "(cm.sender.userID = :userId2 AND cm.receiver.userID = :userId1) " +
           "ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findMessagesBetweenUsers(@Param("userId1") Integer userId1, 
                                             @Param("userId2") Integer userId2, 
                                             Pageable pageable);

    /**
     * Lấy tin nhắn chưa đọc của user
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.receiver.userID = :userId ORDER BY cm.sentAt DESC")
    List<ChatMessage> findUnreadMessagesByUserId(@Param("userId") Integer userId);

    /**
     * Đếm số tin nhắn chưa đọc
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.receiver.userID = :userId")
    Long countUnreadMessagesByUserId(@Param("userId") Integer userId);

    /**
     * Lấy tin nhắn mới nhất giữa hai user
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "(cm.sender.userID = :userId1 AND cm.receiver.userID = :userId2) OR " +
           "(cm.sender.userID = :userId2 AND cm.receiver.userID = :userId1) " +
           "ORDER BY cm.sentAt DESC")
    Optional<ChatMessage> findLatestMessageBetweenUsers(@Param("userId1") Integer userId1, 
                                                       @Param("userId2") Integer userId2);

    /**
     * Lấy tin nhắn trong khoảng thời gian (cho real-time sync)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "((cm.sender.userID = :userId1 AND cm.receiver.userID = :userId2) OR " +
           "(cm.sender.userID = :userId2 AND cm.receiver.userID = :userId1)) " +
           "AND cm.sentAt > :since ORDER BY cm.sentAt ASC")
    List<ChatMessage> findMessagesSince(@Param("userId1") Integer userId1, 
                                       @Param("userId2") Integer userId2, 
                                       @Param("since") LocalDateTime since);

    /**
     * Lấy tất cả tin nhắn của user (cho admin)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "cm.sender.userID = :userId OR cm.receiver.userID = :userId " +
           "ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findAllMessagesByUserId(@Param("userId") Integer userId, Pageable pageable);
}


