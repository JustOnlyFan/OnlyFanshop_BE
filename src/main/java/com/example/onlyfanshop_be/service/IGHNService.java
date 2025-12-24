package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ghn.*;

import java.util.List;

/**
 * IGHNService - Interface cho GHN API Integration
 * Quản lý tích hợp với Giao Hàng Nhanh API cho vận chuyển nội bộ
 */
public interface IGHNService {
    
    /**
     * Tạo đơn vận chuyển GHN
     * @param request Thông tin đơn hàng
     * @return Response từ GHN API với order code
     */
    GHNCreateOrderResponse createOrder(GHNCreateOrderRequest request);
    
    /**
     * Lấy trạng thái đơn hàng từ GHN
     * @param orderCode Mã đơn hàng GHN
     * @return Trạng thái đơn hàng
     */
    GHNOrderStatus getOrderStatus(String orderCode);
    
    /**
     * Lấy chi tiết đơn hàng từ GHN
     * @param orderCode Mã đơn hàng GHN
     * @return Chi tiết đơn hàng
     */
    GHNOrderDetailResponse getOrderDetail(String orderCode);
    
    /**
     * Validate cấu hình GHN bằng cách gọi API test
     * @return Kết quả validation
     */
    GHNValidationResult validateConfiguration();
    
    /**
     * Validate cấu hình GHN với token và shopId cụ thể
     * @param apiToken Token API
     * @param shopId Shop ID
     * @return Kết quả validation
     */
    GHNValidationResult validateConfiguration(String apiToken, String shopId);
    
    /**
     * Lấy danh sách tỉnh/thành phố
     * @return Danh sách tỉnh/thành
     */
    List<GHNProvinceResponse> getProvinces();
    
    /**
     * Lấy danh sách quận/huyện theo tỉnh
     * @param provinceId ID tỉnh/thành
     * @return Danh sách quận/huyện
     */
    List<GHNDistrictResponse> getDistricts(Integer provinceId);
    
    /**
     * Lấy danh sách phường/xã theo quận
     * @param districtId ID quận/huyện
     * @return Danh sách phường/xã
     */
    List<GHNWardResponse> getWards(Integer districtId);
    
    /**
     * Tính phí vận chuyển
     * @param request Thông tin để tính phí
     * @return Kết quả tính phí
     */
    GHNCalculateFeeResponse calculateShippingFee(GHNFeeRequest request);
    
    /**
     * Hủy đơn vận chuyển
     * @param orderCode Mã đơn hàng GHN
     * @return true nếu hủy thành công
     */
    boolean cancelOrder(String orderCode);
    
    /**
     * Lấy thời gian giao hàng dự kiến
     * @param fromDistrictId ID quận gửi
     * @param fromWardCode Mã phường gửi
     * @param toDistrictId ID quận nhận
     * @param toWardCode Mã phường nhận
     * @param serviceTypeId Loại dịch vụ
     * @return Thời gian dự kiến
     */
    String getLeadTime(Integer fromDistrictId, String fromWardCode,
                       Integer toDistrictId, String toWardCode, Integer serviceTypeId);
}
