package com.example.onlyfanshop_be.service.shipping;

import com.example.onlyfanshop_be.config.GHNConfig;
import com.example.onlyfanshop_be.dto.CreateShipmentRequest;
import com.example.onlyfanshop_be.dto.ghn.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNShippingService implements ShippingService {
    
    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));
        return headers;
    }
    
    @Override
    public GHNCreateOrderResponse createOrder(CreateShipmentRequest request) {
        String url = ghnConfig.getApiUrl() + "/v2/shipping-order/create";
        
        GHNCreateOrderRequest ghnRequest = GHNCreateOrderRequest.builder()
            .paymentTypeId(request.getPaymentTypeId() != null ? request.getPaymentTypeId() : 1)
            .note(request.getNote())
            .requiredNote(request.getRequiredNote() != null ? request.getRequiredNote() : "KHONGCHOXEMHANG")
            .fromName(request.getFromName())
            .fromPhone(request.getFromPhone())
            .fromAddress(request.getFromAddress())
            .toName(request.getToName())
            .toPhone(request.getToPhone())
            .toAddress(request.getToAddress())
            .toWardCode(request.getToWardCode())
            .toDistrictId(request.getToDistrictId())
            .codAmount(request.getCodAmount() != null ? request.getCodAmount().intValue() : 0)
            .weight(request.getWeight() != null ? request.getWeight() : 500)
            .length(request.getLength() != null ? request.getLength() : 20)
            .width(request.getWidth() != null ? request.getWidth() : 20)
            .height(request.getHeight() != null ? request.getHeight() : 10)
            .insuranceValue(request.getInsuranceValue() != null ? request.getInsuranceValue().intValue() : 0)
            .serviceTypeId(request.getServiceTypeId() != null ? request.getServiceTypeId() : 2)
            .items(request.getItems() != null ? request.getItems().stream()
                .map(item -> GHNCreateOrderRequest.GHNItem.builder()
                    .name(item.getName())
                    .code(item.getCode())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .weight(item.getWeight())
                    .build())
                .collect(Collectors.toList()) : Collections.emptyList())
            .build();
        
        try {
            HttpEntity<GHNCreateOrderRequest> entity = new HttpEntity<>(ghnRequest, createHeaders());
            ResponseEntity<GHNResponse<GHNCreateOrderResponse>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<GHNCreateOrderResponse>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            log.error("GHN create order failed: {}", response.getBody());
            throw new RuntimeException("Failed to create GHN order: " + 
                (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
        } catch (Exception e) {
            log.error("Error creating GHN order", e);
            throw new RuntimeException("Error creating GHN order: " + e.getMessage(), e);
        }
    }
    
    @Override
    public GHNCalculateFeeResponse calculateFee(GHNCalculateFeeRequest request) {
        String url = ghnConfig.getApiUrl() + "/v2/shipping-order/fee";
        
        try {
            HttpEntity<GHNCalculateFeeRequest> entity = new HttpEntity<>(request, createHeaders());
            ResponseEntity<GHNResponse<GHNCalculateFeeResponse>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<GHNCalculateFeeResponse>>() {}
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            log.error("GHN calculate fee failed: {}", response.getBody());
            throw new RuntimeException("Failed to calculate GHN fee");
        } catch (Exception e) {
            log.error("Error calculating GHN fee", e);
            throw new RuntimeException("Error calculating GHN fee: " + e.getMessage(), e);
        }
    }
    
    @Override
    public GHNOrderDetailResponse getOrderDetail(String orderCode) {
        String url = ghnConfig.getApiUrl() + "/v2/shipping-order/detail";
        
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
            return null;
        } catch (Exception e) {
            log.error("Error getting GHN order detail", e);
            return null;
        }
    }
    
    @Override
    public boolean cancelOrder(String orderCode) {
        String url = ghnConfig.getApiUrl() + "/v2/switch-status/cancel";
        
        try {
            Map<String, Object> body = Map.of("order_codes", List.of(orderCode));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            ResponseEntity<GHNResponse<Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<GHNResponse<Object>>() {}
            );
            
            return response.getBody() != null && response.getBody().isSuccess();
        } catch (Exception e) {
            log.error("Error canceling GHN order", e);
            return false;
        }
    }
    
    @Override
    public List<GHNProvinceResponse> getProvinces() {
        String url = ghnConfig.getApiUrl() + "/master-data/province";
        
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
        } catch (Exception e) {
            log.error("Error getting GHN provinces", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<GHNDistrictResponse> getDistricts(Integer provinceId) {
        String url = ghnConfig.getApiUrl() + "/master-data/district";
        
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
        } catch (Exception e) {
            log.error("Error getting GHN districts", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<GHNWardResponse> getWards(Integer districtId) {
        String url = ghnConfig.getApiUrl() + "/master-data/ward?district_id=" + districtId;
        
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
        } catch (Exception e) {
            log.error("Error getting GHN wards", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public String getLeadTime(Integer fromDistrictId, String fromWardCode,
                              Integer toDistrictId, String toWardCode, Integer serviceTypeId) {
        String url = ghnConfig.getApiUrl() + "/v2/shipping-order/leadtime";
        
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
        } catch (Exception e) {
            log.error("Error getting GHN leadtime", e);
            return null;
        }
    }
}
