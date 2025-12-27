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

    List<InventoryRequest> findByStoreIdOrderByCreatedAtDesc(Integer storeId);

    @Query("SELECT ir FROM InventoryRequest ir WHERE ir.status = 'PENDING' ORDER BY ir.createdAt ASC")
    List<InventoryRequest> findPendingRequests();

    Long countByStatus(InventoryRequestStatus status);

    Page<InventoryRequest> findByStatus(InventoryRequestStatus status, Pageable pageable);
}
