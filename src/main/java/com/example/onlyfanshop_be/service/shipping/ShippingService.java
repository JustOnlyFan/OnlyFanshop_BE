package com.example.onlyfanshop_be.service.shipping;

import com.example.onlyfanshop_be.dto.CreateShipmentRequest;
import com.example.onlyfanshop_be.dto.ghn.*;

import java.util.List;

/**
 * Interface cho các dịch vụ vận chuyển
 * Sử dụng Strategy Pattern để dễ dàng thêm các đơn vị vận chuyển khác
 */
public interface ShippingService {
    
    /**
     * Tạo đơn vận chuyển
     */
    GHNCreateOrderResponse createOrder(CreateShipmentRequest request);
    
    /**
     * Tính phí vận chuyển
     */
    GHNCalculateFeeResponse calculateFee(GHNCalculateFeeRequest request);
    
    /**
     * Lấy thông tin đơn vận chuyển
     */
    GHNOrderDetailResponse getOrderDetail(String orderCode);
    
    /**
     * Hủy đơn vận chuyển
     */
    boolean cancelOrder(String orderCode);
    
    /**
     * Lấy danh sách tỉnh/thành phố
     */
    List<GHNProvinceResponse> getProvinces();
    
    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    List<GHNDistrictResponse> getDistricts(Integer provinceId);
    
    /**
     * Lấy danh sách phường/xã theo quận
     */
    List<GHNWardResponse> getWards(Integer districtId);
    
    /**
     * Lấy thời gian giao hàng dự kiến
     */
    String getLeadTime(Integer fromDistrictId, String fromWardCode, 
                       Integer toDistrictId, String toWardCode, Integer serviceTypeId);
}
