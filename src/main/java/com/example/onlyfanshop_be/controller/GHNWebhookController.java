package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller nhận webhook từ GHN để cập nhật trạng thái đơn hàng
 * Cần cấu hình URL này trong GHN Dashboard
 */
@RestController
@RequestMapping("/api/webhooks/ghn")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GHN Webhook", description = "Webhook nhận cập nhật từ GHN")
public class GHNWebhookController {
    
    private final ShipmentService shipmentService;
    
    /**
     * Webhook endpoint để GHN gọi khi có cập nhật trạng thái
     * GHN sẽ POST data với format:
     * {
     *   "OrderCode": "GHN_ORDER_CODE",
     *   "Status": "delivering",
     *   "Time": "2024-01-15T10:30:00Z",
     *   ...
     * }
     */
    @PostMapping("/status")
    @Operation(summary = "Webhook nhận cập nhật trạng thái từ GHN")
    public ResponseEntity<Map<String, String>> handleStatusUpdate(@RequestBody Map<String, Object> payload) {
        log.info("Received GHN webhook: {}", payload);
        
        try {
            String orderCode = (String) payload.get("OrderCode");
            String status = (String) payload.get("Status");
            
            if (orderCode != null && status != null) {
                shipmentService.updateStatusFromWebhook(orderCode, status);
                log.info("Updated shipment status: {} -> {}", orderCode, status);
            }
            
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Error processing GHN webhook", e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
