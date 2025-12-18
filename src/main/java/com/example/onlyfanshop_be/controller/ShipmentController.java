package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.CreateShipmentRequest;
import com.example.onlyfanshop_be.dto.ShipmentDTO;
import com.example.onlyfanshop_be.dto.ghn.*;
import com.example.onlyfanshop_be.enums.ShipmentStatus;
import com.example.onlyfanshop_be.enums.ShipmentType;
import com.example.onlyfanshop_be.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipment", description = "API quản lý vận chuyển")
public class ShipmentController {
    
    private final ShipmentService shipmentService;
    
    @PostMapping
    @Operation(summary = "Tạo đơn vận chuyển mới")
    public ResponseEntity<ShipmentDTO> createShipment(@RequestBody CreateShipmentRequest request) {
        ShipmentDTO shipment = shipmentService.createShipment(request);
        return ResponseEntity.ok(shipment);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin shipment theo ID")
    public ResponseEntity<ShipmentDTO> getShipmentById(@PathVariable Long id) {
        return shipmentService.getShipmentById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Lấy shipment theo order ID")
    public ResponseEntity<ShipmentDTO> getShipmentByOrderId(@PathVariable Long orderId) {
        return shipmentService.getShipmentByOrderId(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/inventory-request/{inventoryRequestId}")
    @Operation(summary = "Lấy shipment theo inventory request ID")
    public ResponseEntity<ShipmentDTO> getShipmentByInventoryRequestId(@PathVariable Long inventoryRequestId) {
        return shipmentService.getShipmentByInventoryRequestId(inventoryRequestId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Lấy shipment theo tracking number")
    public ResponseEntity<ShipmentDTO> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        return shipmentService.getShipmentByTrackingNumber(trackingNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "Lấy danh sách shipment theo loại")
    public ResponseEntity<Page<ShipmentDTO>> getShipments(
            @RequestParam(required = false) ShipmentType type,
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<ShipmentDTO> result;
        if (type != null && status != null) {
            result = shipmentService.getShipmentsByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            result = shipmentService.getShipmentsByType(type, pageable);
        } else {
            result = shipmentService.getShipmentsByType(ShipmentType.CUSTOMER_DELIVERY, pageable);
        }
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy shipment")
    public ResponseEntity<Map<String, Object>> cancelShipment(@PathVariable Long id) {
        boolean success = shipmentService.cancelShipment(id);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Đã hủy đơn vận chuyển" : "Không thể hủy đơn vận chuyển"
        ));
    }
    
    @PostMapping("/{id}/sync")
    @Operation(summary = "Đồng bộ trạng thái từ GHN")
    public ResponseEntity<Map<String, String>> syncStatus(@PathVariable Long id) {
        shipmentService.syncStatusFromGHN(id);
        return ResponseEntity.ok(Map.of("message", "Đã đồng bộ trạng thái"));
    }
    
    // === GHN Address APIs ===
    
    @GetMapping("/ghn/provinces")
    @Operation(summary = "Lấy danh sách tỉnh/thành phố từ GHN")
    public ResponseEntity<List<GHNProvinceResponse>> getProvinces() {
        return ResponseEntity.ok(shipmentService.getProvinces());
    }
    
    @GetMapping("/ghn/districts")
    @Operation(summary = "Lấy danh sách quận/huyện theo tỉnh")
    public ResponseEntity<List<GHNDistrictResponse>> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(shipmentService.getDistricts(provinceId));
    }
    
    @GetMapping("/ghn/wards")
    @Operation(summary = "Lấy danh sách phường/xã theo quận")
    public ResponseEntity<List<GHNWardResponse>> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(shipmentService.getWards(districtId));
    }
    
    // === Calculate Fee ===
    
    @PostMapping("/calculate-fee")
    @Operation(summary = "Tính phí vận chuyển")
    public ResponseEntity<GHNCalculateFeeResponse> calculateFee(@RequestBody GHNCalculateFeeRequest request) {
        return ResponseEntity.ok(shipmentService.calculateShippingFee(request));
    }
}
