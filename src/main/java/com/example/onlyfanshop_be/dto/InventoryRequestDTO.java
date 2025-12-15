package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.InventoryRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequestDTO {
    private Long id;
    private Integer storeId;
    private String storeName;
    
    // Danh sách sản phẩm trong yêu cầu
    private List<InventoryRequestItemDTO> items;
    private Integer totalItems; // Số lượng loại sản phẩm
    private Integer totalQuantity; // Tổng số lượng
    
    // Legacy fields - giữ lại để tương thích ngược
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer requestedQuantity;
    private Integer approvedQuantity;
    
    private InventoryRequestStatus status;
    private Long requestedBy;
    private String requesterName;
    private Long approvedBy;
    private String approverName;
    private String requestNote;
    private String adminNote;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
