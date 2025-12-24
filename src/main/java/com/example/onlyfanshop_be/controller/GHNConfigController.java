package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.ghn.*;
import com.example.onlyfanshop_be.service.IGHNConfigService;
import com.example.onlyfanshop_be.service.IGHNService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GHNConfigController - REST API cho quản lý cấu hình GHN
 */
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GHN Configuration", description = "APIs for GHN configuration management")
public class GHNConfigController {
    
    private final IGHNConfigService ghnConfigService;
    private final IGHNService ghnService;
    
    /**
     * GET /api/ghn/config - Lấy cấu hình GHN hiện tại
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get current GHN configuration")
    public ResponseEntity<GHNConfigDTO> getConfiguration() {
        log.info("Getting GHN configuration");
        GHNConfigDTO config = ghnConfigService.getConfiguration();
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        // Mask the token for security
        config.setApiToken(config.getMaskedToken());
        return ResponseEntity.ok(config);
    }
    
    /**
     * PUT /api/ghn/config - Cập nhật cấu hình GHN
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update GHN configuration")
    public ResponseEntity<GHNConfigDTO> updateConfiguration(
            @Valid @RequestBody GHNConfigUpdateRequest request) {
        log.info("Updating GHN configuration");
        GHNConfigDTO config = ghnConfigService.updateConfiguration(request);
        // Mask the token for security
        config.setApiToken(config.getMaskedToken());
        return ResponseEntity.ok(config);
    }
    
    /**
     * POST /api/ghn/config/validate - Validate cấu hình GHN
     */
    @PostMapping("/config/validate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Validate GHN configuration")
    public ResponseEntity<GHNValidationResult> validateConfiguration(
            @RequestBody(required = false) GHNConfigUpdateRequest request) {
        log.info("Validating GHN configuration");
        
        GHNValidationResult result;
        if (request != null && request.getApiToken() != null && request.getShopId() != null) {
            // Validate with provided credentials
            result = ghnConfigService.validateConfiguration(request.getApiToken(), request.getShopId());
        } else {
            // Validate current configuration
            result = ghnConfigService.validateCurrentConfiguration();
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * GET /api/ghn/provinces - Lấy danh sách tỉnh/thành phố
     */
    @GetMapping("/provinces")
    @Operation(summary = "Get list of provinces")
    public ResponseEntity<List<GHNProvinceResponse>> getProvinces() {
        log.debug("Getting GHN provinces");
        List<GHNProvinceResponse> provinces = ghnService.getProvinces();
        return ResponseEntity.ok(provinces);
    }
    
    /**
     * GET /api/ghn/districts/{provinceId} - Lấy danh sách quận/huyện theo tỉnh
     */
    @GetMapping("/districts/{provinceId}")
    @Operation(summary = "Get list of districts by province")
    public ResponseEntity<List<GHNDistrictResponse>> getDistricts(
            @PathVariable Integer provinceId) {
        log.debug("Getting GHN districts for province: {}", provinceId);
        List<GHNDistrictResponse> districts = ghnService.getDistricts(provinceId);
        return ResponseEntity.ok(districts);
    }
    
    /**
     * GET /api/ghn/wards/{districtId} - Lấy danh sách phường/xã theo quận
     */
    @GetMapping("/wards/{districtId}")
    @Operation(summary = "Get list of wards by district")
    public ResponseEntity<List<GHNWardResponse>> getWards(
            @PathVariable Integer districtId) {
        log.debug("Getting GHN wards for district: {}", districtId);
        List<GHNWardResponse> wards = ghnService.getWards(districtId);
        return ResponseEntity.ok(wards);
    }
    
    /**
     * POST /api/ghn/calculate-fee - Tính phí vận chuyển
     */
    @PostMapping("/calculate-fee")
    @Operation(summary = "Calculate shipping fee")
    public ResponseEntity<GHNCalculateFeeResponse> calculateFee(
            @Valid @RequestBody GHNFeeRequest request) {
        log.debug("Calculating GHN shipping fee");
        GHNCalculateFeeResponse response = ghnService.calculateShippingFee(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/ghn/order/{orderCode} - Lấy chi tiết đơn hàng GHN
     */
    @GetMapping("/order/{orderCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get GHN order detail")
    public ResponseEntity<GHNOrderDetailResponse> getOrderDetail(
            @PathVariable String orderCode) {
        log.debug("Getting GHN order detail: {}", orderCode);
        GHNOrderDetailResponse detail = ghnService.getOrderDetail(orderCode);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }
    
    /**
     * GET /api/ghn/order/{orderCode}/status - Lấy trạng thái đơn hàng GHN
     */
    @GetMapping("/order/{orderCode}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get GHN order status")
    public ResponseEntity<GHNOrderStatus> getOrderStatus(
            @PathVariable String orderCode) {
        log.debug("Getting GHN order status: {}", orderCode);
        GHNOrderStatus status = ghnService.getOrderStatus(orderCode);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
