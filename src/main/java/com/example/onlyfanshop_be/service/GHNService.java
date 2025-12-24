package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ghn.*;
import com.example.onlyfanshop_be.entity.GHNConfiguration;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.GHNConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GHNService - Implementation của IGHNService
 * Tích hợp với GHN API cho vận chuyển nội bộ giữa các kho
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GHNService implements IGHNService {
    
    private final RestTemplate restTemplate;
    private final GHNConfigurationRepository ghnConfigurationRepository;
    
    @Value("${GHN_API_URL:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String defaultApiUrl;
    
    /**
     * Lấy cấu hình GHN đang active
     */
    private GHNConfiguration getActiveConfiguration() {
        return ghnConfigurationRepository.findFirstByIsActiveTrue()
            .orElseThrow(() -> new AppException(ErrorCode.GHN_CONFIG_NOT_FOUND));
    }
    
    /**
     * Tạo HTTP headers với token và shopId
     */
    private HttpHeaders createHeaders() {
        GHNConfiguration config = getActiveConfiguration();
        return createHeaders(config.getApiToken(), config.getShopId());
    }
    
    /**
     * Tạo HTTP headers với token và shopId cụ thể
     */
    private HttpHeaders createHeaders(String token, String shopId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        return headers;
    }
    
    /**
     * Lấy API URL
     */
    private String getApiUrl() {
        return defaultApiUrl;
    }
    
    @Override
    public GHNCreateOrderResponse createOrder(GHNCreateOrderRequest request) {
        String url = getApiUrl() + "/v2/shipping-order/create";
        
        try {
            HttpEntity<GHNCreateOrderRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<GHNResponse<GHNCreateOrderResponse>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<GHNCreateOrderResponse>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                log.info("GHN order created successfully: {}", response.getBody().getData().getOrderCode());
                return response.getBody().getData();
            }
            
            String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "Unknown error";
            log.error("GHN create order failed: {}", errorMsg);
            throw new AppException(ErrorCode.GHN_API_ERROR);
        } catch (RestClientException e) {
            log.error("Error calling GHN create order API", e);
            throw new AppException(ErrorCode.GHN_API_ERROR);
        }
    }
    
    @Override
    public GHNOrderStatus getOrderStatus(String orderCode) {
        GHNOrderDetailResponse detail = getOrderDetail(orderCode);
        if (detail != null && detail.getStatus() != null) {
            return GHNOrderStatus.fromCode(detail.getStatus());
        }
        return null;
    }
    
    @Override
    public GHNOrderDetailResponse getOrderDetail(String orderCode) {
        String url = getApiUrl() + "/v2/shipping-order/detail";
        
        try {
            Map<String, String> body = Map.of("order_code", orderCode);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<GHNResponse<GHNOrderDetailResponse>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<GHNOrderDetailResponse>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            
            log.warn("GHN get order detail failed for order: {}", orderCode);
            return null;
        } catch (RestClientException e) {
            log.error("Error calling GHN get order detail API", e);
            return null;
        }
    }
    
    @Override
    public GHNValidationResult validateConfiguration() {
        GHNConfiguration config = getActiveConfiguration();
        return validateConfiguration(config.getApiToken(), config.getShopId());
    }
    
    @Override
    public GHNValidationResult validateConfiguration(String apiToken, String shopId) {
        String url = getApiUrl() + "/v2/shop/all";
        
        try {
            HttpHeaders headers = createHeaders(apiToken, shopId);
            Map<String, Object> body = Map.of("offset", 0, "limit", 1);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<GHNResponse<GHNShopListResponse>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<GHNShopListResponse>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                GHNShopListResponse data = response.getBody().getData();
                if (data != null && data.getShops() != null && !data.getShops().isEmpty()) {
                    GHNShopInfoResponse shop = data.getShops().get(0);
                    GHNValidationResult.GHNShopInfo shopInfo = GHNValidationResult.GHNShopInfo.builder()
                        .shopId(shop.getId())
                        .name(shop.getName())
                        .phone(shop.getPhone())
                        .address(shop.getAddress())
                        .districtId(shop.getDistrictId())
                        .wardCode(shop.getWardCode())
                        .build();
                    return GHNValidationResult.success(shopInfo);
                }
                return GHNValidationResult.success(null);
            }
            
            String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "Unknown error";
            return GHNValidationResult.failure(errorMsg);
        } catch (RestClientException e) {
            log.error("Error validating GHN configuration", e);
            return GHNValidationResult.failure("Failed to connect to GHN API: " + e.getMessage());
        }
    }
    
    @Override
    public List<GHNProvinceResponse> getProvinces() {
        String url = getApiUrl() + "/master-data/province";
        
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<GHNResponse<List<GHNProvinceResponse>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<GHNResponse<List<GHNProvinceResponse>>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error getting GHN provinces", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<GHNDistrictResponse> getDistricts(Integer provinceId) {
        String url = getApiUrl() + "/master-data/district";
        
        try {
            Map<String, Integer> body = Map.of("province_id", provinceId);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<GHNResponse<List<GHNDistrictResponse>>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<List<GHNDistrictResponse>>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error getting GHN districts", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<GHNWardResponse> getWards(Integer districtId) {
        String url = getApiUrl() + "/master-data/ward?district_id=" + districtId;
        
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<GHNResponse<List<GHNWardResponse>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<GHNResponse<List<GHNWardResponse>>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error getting GHN wards", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public GHNCalculateFeeResponse calculateShippingFee(GHNFeeRequest request) {
        String url = getApiUrl() + "/v2/shipping-order/fee";
        
        try {
            GHNCalculateFeeRequest calcRequest = request.toCalculateFeeRequest();
            HttpEntity<GHNCalculateFeeRequest> entity = new HttpEntity<>(calcRequest, createHeaders());
            ResponseEntity<GHNResponse<GHNCalculateFeeResponse>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<GHNCalculateFeeResponse>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            
            log.error("GHN calculate fee failed: {}", response.getBody());
            throw new AppException(ErrorCode.GHN_API_ERROR);
        } catch (RestClientException e) {
            log.error("Error calculating GHN shipping fee", e);
            throw new AppException(ErrorCode.GHN_API_ERROR);
        }
    }
    
    @Override
    public boolean cancelOrder(String orderCode) {
        String url = getApiUrl() + "/v2/switch-status/cancel";
        
        try {
            Map<String, Object> body = Map.of("order_codes", List.of(orderCode));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<GHNResponse<Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<Object>>() {}
            );
            
            boolean success = response.getBody() != null && response.getBody().isSuccess();
            if (success) {
                log.info("GHN order cancelled successfully: {}", orderCode);
            } else {
                log.warn("GHN cancel order failed for: {}", orderCode);
            }
            return success;
        } catch (RestClientException e) {
            log.error("Error canceling GHN order", e);
            return false;
        }
    }
    
    @Override
    public String getLeadTime(Integer fromDistrictId, String fromWardCode,
                              Integer toDistrictId, String toWardCode, Integer serviceTypeId) {
        String url = getApiUrl() + "/v2/shipping-order/leadtime";
        
        try {
            Map<String, Object> body = Map.of(
                "from_district_id", fromDistrictId,
                "from_ward_code", fromWardCode,
                "to_district_id", toDistrictId,
                "to_ward_code", toWardCode,
                "service_id", serviceTypeId
            );
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<GHNResponse<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<Map<String, Object>>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess() && response.getBody().getData() != null) {
                Object leadtime = response.getBody().getData().get("leadtime");
                return leadtime != null ? leadtime.toString() : null;
            }
            return null;
        } catch (RestClientException e) {
            log.error("Error getting GHN leadtime", e);
            return null;
        }
    }
    
    /**
     * Inner class for shop list response
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class GHNShopListResponse {
        private List<GHNShopInfoResponse> shops;
        private Integer total;
    }
}
