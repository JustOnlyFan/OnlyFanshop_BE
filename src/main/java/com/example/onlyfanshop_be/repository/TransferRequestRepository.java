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

    Page<TransferRequest> findByStatusOrderByCreatedAtDesc(TransferRequestStatus status, Pageable pageable);

    Page<TransferRequest> findByStoreIdOrderByCreatedAtDesc(Integer storeId, Pageable pageable);

    Page<TransferRequest> findByStoreIdAndStatusOrderByCreatedAtDesc(
        Integer storeId, 
        TransferRequestStatus status, 
        Pageable pageable
    );

}
