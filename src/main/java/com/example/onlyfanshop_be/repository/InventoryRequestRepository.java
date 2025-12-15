package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InventoryRequest;
import com.example.onlyfanshop_be.enums.InventoryRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRequestRepository extends JpaRepository<InventoryRequest, Long> {
    
    /**
     * Tìm tất cả requests của một store
     */
    List<InventoryRequest> findByStoreIdOrderByCreatedAtDesc(Integer storeId);
    
    /**
     * Tìm tất cả requests theo status
     */
    List<InventoryRequest> findByStatusOrderByCreatedAtDesc(InventoryRequestStatus status);
    
    /**
     * Tìm tất cả requests của một store theo status
     */
    List<InventoryRequest> findByStoreIdAndStatusOrderByCreatedAtDesc(Integer storeId, InventoryRequestStatus status);
    
    /**
     * Tìm requests pending (chờ duyệt)
     */
    @Query("SELECT ir FROM InventoryRequest ir WHERE ir.status = 'PENDING' ORDER BY ir.createdAt ASC")
    List<InventoryRequest> findPendingRequests();
    
    /**
     * Đếm số requests pending của một store
     */
    Long countByStoreIdAndStatus(Integer storeId, InventoryRequestStatus status);
    
    /**
     * Đếm tổng số requests pending
     */
    Long countByStatus(InventoryRequestStatus status);
    
    /**
     * Tìm requests với pagination
     */
    Page<InventoryRequest> findByStoreId(Integer storeId, Pageable pageable);
    
    /**
     * Tìm requests theo status với pagination
     */
    Page<InventoryRequest> findByStatus(InventoryRequestStatus status, Pageable pageable);
    
    /**
     * Tìm requests của một product tại một store
     */
    List<InventoryRequest> findByStoreIdAndProductIdOrderByCreatedAtDesc(Integer storeId, Long productId);
    
    /**
     * Kiểm tra xem có request pending cho product tại store không
     */
    @Query("SELECT COUNT(ir) > 0 FROM InventoryRequest ir WHERE ir.storeId = :storeId AND ir.productId = :productId AND ir.status = 'PENDING'")
    boolean existsPendingRequest(@Param("storeId") Integer storeId, @Param("productId") Long productId);
}
