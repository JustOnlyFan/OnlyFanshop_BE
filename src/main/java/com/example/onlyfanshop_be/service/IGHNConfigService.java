package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ghn.GHNConfigDTO;
import com.example.onlyfanshop_be.dto.ghn.GHNConfigUpdateRequest;
import com.example.onlyfanshop_be.dto.ghn.GHNValidationResult;

/**
 * IGHNConfigService - Interface cho quản lý cấu hình GHN
 */
public interface IGHNConfigService {
    
    /**
     * Lấy cấu hình GHN hiện tại
     * @return Cấu hình GHN
     */
    GHNConfigDTO getConfiguration();
    
    /**
     * Cập nhật cấu hình GHN
     * @param request Thông tin cấu hình mới
     * @return Cấu hình đã cập nhật
     */
    GHNConfigDTO updateConfiguration(GHNConfigUpdateRequest request);
    
    /**
     * Validate cấu hình GHN hiện tại
     * @return Kết quả validation
     */
    GHNValidationResult validateCurrentConfiguration();
    
    /**
     * Validate cấu hình GHN với thông tin cụ thể
     * @param apiToken Token API
     * @param shopId Shop ID
     * @return Kết quả validation
     */
    GHNValidationResult validateConfiguration(String apiToken, String shopId);
    
    /**
     * Kiểm tra xem có cấu hình active không
     * @return true nếu có cấu hình active
     */
    boolean hasActiveConfiguration();
}
