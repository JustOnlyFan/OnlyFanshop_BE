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

    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "(cm.sender.id = :userId1 AND cm.receiver.id = :userId2) OR " +
           "(cm.sender.id = :userId2 AND cm.receiver.id = :userId1) " +
           "ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findMessagesBetweenUsers(@Param("userId1") Long userId1, 
                                             @Param("userId2") Long userId2, 
                                             Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.receiver.id = :userId ORDER BY cm.sentAt DESC")
    List<ChatMessage> findUnreadMessagesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.receiver.id = :userId")
    Long countUnreadMessagesByUserId(@Param("userId") Long userId);

    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "(cm.sender.id = :userId1 AND cm.receiver.id = :userId2) OR " +
           "(cm.sender.id = :userId2 AND cm.receiver.id = :userId1) " +
           "ORDER BY cm.sentAt DESC")
    Optional<ChatMessage> findLatestMessageBetweenUsers(@Param("userId1") Long userId1, 
                                                       @Param("userId2") Long userId2);

    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "((cm.sender.id = :userId1 AND cm.receiver.id = :userId2) OR " +
           "(cm.sender.id = :userId2 AND cm.receiver.id = :userId1)) " +
           "AND cm.sentAt > :since ORDER BY cm.sentAt ASC")
    List<ChatMessage> findMessagesSince(@Param("userId1") Long userId1, 
                                       @Param("userId2") Long userId2, 
                                       @Param("since") LocalDateTime since);

    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "cm.sender.id = :userId OR cm.receiver.id = :userId " +
           "ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findAllMessagesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default Page<ChatMessage> findMessagesBetweenUsers(Integer userId1, Integer userId2, Pageable pageable) {
        return findMessagesBetweenUsers(
            userId1 != null ? userId1.longValue() : null,
            userId2 != null ? userId2.longValue() : null,
            pageable
        );
    }
    
    @Deprecated
    default List<ChatMessage> findUnreadMessagesByUserId(Integer userId) {
        return findUnreadMessagesByUserId(userId != null ? userId.longValue() : null);
    }
    
    @Deprecated
    default Long countUnreadMessagesByUserId(Integer userId) {
        return countUnreadMessagesByUserId(userId != null ? userId.longValue() : null);
    }
    
    @Deprecated
    default Optional<ChatMessage> findLatestMessageBetweenUsers(Integer userId1, Integer userId2) {
        return findLatestMessageBetweenUsers(
            userId1 != null ? userId1.longValue() : null,
            userId2 != null ? userId2.longValue() : null
        );
    }
    
    @Deprecated
    default List<ChatMessage> findMessagesSince(Integer userId1, Integer userId2, LocalDateTime since) {
        return findMessagesSince(
            userId1 != null ? userId1.longValue() : null,
            userId2 != null ? userId2.longValue() : null,
            since
        );
    }
    
    @Deprecated
    default Page<ChatMessage> findAllMessagesByUserId(Integer userId, Pageable pageable) {
        return findAllMessagesByUserId(userId != null ? userId.longValue() : null, pageable);
    }
}


