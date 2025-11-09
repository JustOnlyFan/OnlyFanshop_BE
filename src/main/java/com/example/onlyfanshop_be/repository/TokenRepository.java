package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Token;
import com.example.onlyfanshop_be.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    List<Token> findAllByUserIdAndExpiredFalseAndRevokedFalse(Long userId);
    Optional<Token> findByToken(String token);
    void deleteByToken(String token);
    List<Token> findAllByUserIdAndTypeAndExpiredFalseAndRevokedFalse(Long userId, TokenType type);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default List<Token> findAllByUser_UserIDAndExpiredFalseAndRevokedFalse(Integer userID) {
        return findAllByUserIdAndExpiredFalseAndRevokedFalse(userID != null ? userID.longValue() : null);
    }
    
    @Deprecated
    default List<Token> findAllByUser_UserIDAndTypeAndExpiredFalseAndRevokedFalse(Integer userID, TokenType type) {
        return findAllByUserIdAndTypeAndExpiredFalseAndRevokedFalse(userID != null ? userID.longValue() : null, type);
    }
}
