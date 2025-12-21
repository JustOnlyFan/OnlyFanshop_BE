package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.DebtOrder;
import com.example.onlyfanshop_be.enums.DebtOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtOrderRepository extends JpaRepository<DebtOrder, Long> {
    
    /**
     * Tìm debt orders theo status
     */
    Page<DebtOrder> findByStatusOrderByCreatedAtDesc(DebtOrderStatus status, Pageable pageable);
    
    /**
     * Tìm tất cả debt orders theo status
     */
    List<DebtOrder> findByStatus(DebtOrderStatus status);
    
    /**
     * Tìm debt order theo transfer request
     */
    Optional<DebtOrder> findByTransferRequestId(Long transferRequestId);
    
    /**
     * Đếm số debt orders theo status
     */
    long countByStatus(DebtOrderStatus status);
    
    /**
     * Tìm các debt orders PENDING hoặc FULFILLABLE
     */
    List<DebtOrder> findByStatusIn(List<DebtOrderStatus> statuses);
}
