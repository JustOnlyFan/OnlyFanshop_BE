package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for TransferRequest entity
 * Requirements: 3.1, 4.5 - WHEN Staff views their Transfer_Requests THEN the System SHALL display all requests with status, products, quantities, timestamps, and source warehouse info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {
    private Long id;
    private Integer storeId;
    private String storeName;
    
    /**
     * ID của kho nguồn - nơi hàng được chuyển đi
     * Requirements: 3.1 - WHEN Store_Staff creates a Transfer_Request THEN the System SHALL require specifying a source Store_Warehouse
     */
    private Long sourceWarehouseId;
    
    /**
     * Tên của kho nguồn
     * Requirements: 3.1
     */
    private String sourceWarehouseName;
    
    private TransferRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Long processedBy;
    private String processedByName;
    private String rejectReason;
    private List<TransferRequestItemDTO> items;
    
    // Summary fields
    private Integer totalItems;
    private Integer totalQuantity;
}
