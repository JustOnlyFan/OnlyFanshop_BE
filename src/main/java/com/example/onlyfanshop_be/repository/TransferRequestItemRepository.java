package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.TransferRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestItemRepository extends JpaRepository<TransferRequestItem, Long> {
    
    /**
     * Tìm items theo transfer request
     */
    List<TransferRequestItem> findByTransferRequestId(Long transferRequestId);
    
    /**
     * Tìm items theo product
     */
    List<TransferRequestItem> findByProductId(Long productId);
    
    /**
     * Xóa items theo transfer request
     */
    void deleteByTransferRequestId(Long transferRequestId);
    
    /**
     * Tính tổng số lượng đã đặt của một sản phẩm trong các request PENDING
     */
    @Query("SELECT COALESCE(SUM(i.requestedQuantity - i.fulfilledQuantity), 0) " +
           "FROM TransferRequestItem i " +
           "JOIN i.transferRequest r " +
           "WHERE i.productId = :productId " +
           "AND r.status = 'PENDING'")
    Integer getTotalPendingQuantity(@Param("productId") Long productId);
}
