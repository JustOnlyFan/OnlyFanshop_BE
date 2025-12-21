package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.DebtItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtItemRepository extends JpaRepository<DebtItem, Long> {
    
    /**
     * Tìm items theo debt order
     */
    List<DebtItem> findByDebtOrderId(Long debtOrderId);
    
    /**
     * Tìm items theo product
     */
    List<DebtItem> findByProductId(Long productId);
    
    /**
     * Xóa items theo debt order
     */
    void deleteByDebtOrderId(Long debtOrderId);
    
    /**
     * Tìm các debt items chưa được fulfill đầy đủ cho một product
     */
    @Query("SELECT i FROM DebtItem i " +
           "JOIN i.debtOrder d " +
           "WHERE i.productId = :productId " +
           "AND d.status != 'COMPLETED' " +
           "AND i.fulfilledQuantity < i.owedQuantity")
    List<DebtItem> findUnfulfilledByProductId(@Param("productId") Long productId);
    
    /**
     * Tính tổng số lượng còn nợ của một sản phẩm
     */
    @Query("SELECT COALESCE(SUM(i.owedQuantity - i.fulfilledQuantity), 0) " +
           "FROM DebtItem i " +
           "JOIN i.debtOrder d " +
           "WHERE i.productId = :productId " +
           "AND d.status != 'COMPLETED'")
    Integer getTotalOwedQuantity(@Param("productId") Long productId);
}
