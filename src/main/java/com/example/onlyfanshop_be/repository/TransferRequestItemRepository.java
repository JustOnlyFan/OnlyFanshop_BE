package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.TransferRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestItemRepository extends JpaRepository<TransferRequestItem, Long> {

    List<TransferRequestItem> findByTransferRequestId(Long transferRequestId);

    List<TransferRequestItem> findByProductId(Long productId);

}
