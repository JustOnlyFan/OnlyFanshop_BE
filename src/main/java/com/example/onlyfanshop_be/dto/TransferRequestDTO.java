package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {
    private Long id;
    private Integer storeId;
    private String storeName;
    private Long sourceWarehouseId;
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
