package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.TransferRequest;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    
    /**
     * Tìm requests theo status
     */
    Page<TransferRequest> findByStatusOrderByCreatedAtDesc(TransferRequestStatus status, Pageable pageable);
    
    /**
     * Tìm requests theo store
     */
    Page<TransferRequest> findByStoreIdOrderByCreatedAtDesc(Integer storeId, Pageable pageable);
    
    /**
     * Tìm requests theo store và status
     */
    Page<TransferRequest> findByStoreIdAndStatusOrderByCreatedAtDesc(
        Integer storeId, 
        TransferRequestStatus status, 
        Pageable pageable
    );
    
    /**
     * Tìm tất cả requests theo status
     */
    List<TransferRequest> findByStatus(TransferRequestStatus status);
    
    /**
     * Đếm số requests theo status
     */
    long countByStatus(TransferRequestStatus status);
    
    /**
     * Đếm số requests theo store và status
     */
    long countByStoreIdAndStatus(Integer storeId, TransferRequestStatus status);
}
