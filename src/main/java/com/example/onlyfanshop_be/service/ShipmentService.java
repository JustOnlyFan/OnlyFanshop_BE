package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.CreateShipmentRequest;
import com.example.onlyfanshop_be.dto.ShipmentDTO;
import com.example.onlyfanshop_be.dto.ghn.*;
import com.example.onlyfanshop_be.entity.Shipment;
import com.example.onlyfanshop_be.enums.ShipmentStatus;
import com.example.onlyfanshop_be.enums.ShipmentType;
import com.example.onlyfanshop_be.enums.ShippingCarrier;
import com.example.onlyfanshop_be.repository.ShipmentRepository;
import com.example.onlyfanshop_be.service.shipping.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {
    
    private final ShipmentRepository shipmentRepository;
    private final ShippingService shippingService;
    
    /**
     * Tạo shipment và gọi API GHN để tạo đơn vận chuyển
     */
    @Transactional
    public ShipmentDTO createShipment(CreateShipmentRequest request) {
        // Tạo entity
        Shipment shipment = Shipment.builder()
            .shipmentType(request.getShipmentType())
            .orderId(request.getOrderId())
            .inventoryRequestId(request.getInventoryRequestId())
            .carrier(request.getCarrier() != null ? request.getCarrier() : ShippingCarrier.GHN)
            .status(ShipmentStatus.PENDING)
            .fromName(request.getFromName())
            .fromPhone(request.getFromPhone())
            .fromAddress(request.getFromAddress())
            .fromWardCode(request.getFromWardCode())
            .fromDistrictId(request.getFromDistrictId())
            .fromStoreId(request.getFromStoreId())
            .toName(request.getToName())
            .toPhone(request.getToPhone())
            .toAddress(request.getToAddress())
            .toWardCode(request.getToWardCode())
            .toDistrictId(request.getToDistrictId())
            .weight(request.getWeight())
            .length(request.getLength())
            .width(request.getWidth())
            .height(request.getHeight())
            .codAmount(request.getCodAmount() != null ? request.getCodAmount() : BigDecimal.ZERO)
            .insuranceValue(request.getInsuranceValue() != null ? request.getInsuranceValue() : BigDecimal.ZERO)
            .serviceTypeId(request.getServiceTypeId())
            .paymentTypeId(request.getPaymentTypeId())
            .note(request.getNote())
            .requiredNote(request.getRequiredNote())
            .build();
        
        // Gọi GHN API để tạo đơn
        if (request.getCarrier() == null || request.getCarrier() == ShippingCarrier.GHN) {
            try {
                GHNCreateOrderResponse ghnResponse = shippingService.createOrder(request);
                shipment.setCarrierOrderCode(ghnResponse.getOrderCode());
                shipment.setTrackingNumber(ghnResponse.getOrderCode());
                shipment.setShippingFee(BigDecimal.valueOf(ghnResponse.getTotalFee()));
                shipment.setStatus(ShipmentStatus.READY_TO_PICK);
                
                // Parse expected delivery time
                if (ghnResponse.getExpectedDeliveryTime() != null) {
                    try {
                        shipment.setExpectedDeliveryTime(
                            LocalDateTime.parse(ghnResponse.getExpectedDeliveryTime(), 
                                DateTimeFormatter.ISO_DATE_TIME));
                    } catch (Exception e) {
                        log.warn("Could not parse expected delivery time: {}", ghnResponse.getExpectedDeliveryTime());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to create GHN order, saving shipment as PENDING", e);
                // Vẫn lưu shipment nhưng status là PENDING
            }
        }
        
        shipment = shipmentRepository.save(shipment);
        return toDTO(shipment);
    }
    
    /**
     * Tính phí vận chuyển
     */
    public GHNCalculateFeeResponse calculateShippingFee(GHNCalculateFeeRequest request) {
        return shippingService.calculateFee(request);
    }
    
    /**
     * Lấy thông tin shipment theo ID
     */
    public Optional<ShipmentDTO> getShipmentById(Long id) {
        return shipmentRepository.findById(id).map(this::toDTO);
    }
    
    /**
     * Lấy shipment theo order ID
     */
    public Optional<ShipmentDTO> getShipmentByOrderId(Long orderId) {
        return shipmentRepository.findByOrderId(orderId).map(this::toDTO);
    }
    
    /**
     * Lấy shipment theo inventory request ID
     */
    public Optional<ShipmentDTO> getShipmentByInventoryRequestId(Long inventoryRequestId) {
        return shipmentRepository.findByInventoryRequestId(inventoryRequestId).map(this::toDTO);
    }
    
    /**
     * Lấy shipment theo tracking number
     */
    public Optional<ShipmentDTO> getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber).map(this::toDTO);
    }
    
    /**
     * Lấy danh sách shipment theo loại
     */
    public Page<ShipmentDTO> getShipmentsByType(ShipmentType type, Pageable pageable) {
        return shipmentRepository.findByShipmentType(type, pageable).map(this::toDTO);
    }
    
    /**
     * Lấy danh sách shipment theo loại và trạng thái
     */
    public Page<ShipmentDTO> getShipmentsByTypeAndStatus(ShipmentType type, ShipmentStatus status, Pageable pageable) {
        return shipmentRepository.findByTypeAndStatus(type, status, pageable).map(this::toDTO);
    }
    
    /**
     * Cập nhật trạng thái shipment từ GHN webhook
     */
    @Transactional
    public void updateStatusFromWebhook(String orderCode, String ghnStatus) {
        shipmentRepository.findByCarrierOrderCode(orderCode).ifPresent(shipment -> {
            ShipmentStatus newStatus = mapGHNStatus(ghnStatus);
            shipment.setStatus(newStatus);
            
            if (newStatus == ShipmentStatus.PICKED) {
                shipment.setPickedAt(LocalDateTime.now());
            } else if (newStatus == ShipmentStatus.DELIVERED) {
                shipment.setDeliveredAt(LocalDateTime.now());
            }
            
            shipmentRepository.save(shipment);
            log.info("Updated shipment {} status to {}", orderCode, newStatus);
        });
    }
    
    /**
     * Hủy shipment
     */
    @Transactional
    public boolean cancelShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId).map(shipment -> {
            if (shipment.getCarrierOrderCode() != null && shipment.getCarrier() == ShippingCarrier.GHN) {
                boolean cancelled = shippingService.cancelOrder(shipment.getCarrierOrderCode());
                if (cancelled) {
                    shipment.setStatus(ShipmentStatus.CANCEL);
                    shipmentRepository.save(shipment);
                    return true;
                }
                return false;
            }
            shipment.setStatus(ShipmentStatus.CANCEL);
            shipmentRepository.save(shipment);
            return true;
        }).orElse(false);
    }
    
    /**
     * Đồng bộ trạng thái từ GHN
     */
    @Transactional
    public void syncStatusFromGHN(Long shipmentId) {
        shipmentRepository.findById(shipmentId).ifPresent(shipment -> {
            if (shipment.getCarrierOrderCode() != null) {
                GHNOrderDetailResponse detail = shippingService.getOrderDetail(shipment.getCarrierOrderCode());
                if (detail != null) {
                    shipment.setStatus(mapGHNStatus(detail.getStatus()));
                    shipmentRepository.save(shipment);
                }
            }
        });
    }
    
    /**
     * Lấy danh sách tỉnh/thành phố
     */
    public List<GHNProvinceResponse> getProvinces() {
        return shippingService.getProvinces();
    }
    
    /**
     * Lấy danh sách quận/huyện
     */
    public List<GHNDistrictResponse> getDistricts(Integer provinceId) {
        return shippingService.getDistricts(provinceId);
    }
    
    /**
     * Lấy danh sách phường/xã
     */
    public List<GHNWardResponse> getWards(Integer districtId) {
        return shippingService.getWards(districtId);
    }
    
    private ShipmentStatus mapGHNStatus(String ghnStatus) {
        if (ghnStatus == null) return ShipmentStatus.PENDING;
        
        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick" -> ShipmentStatus.READY_TO_PICK;
            case "picking" -> ShipmentStatus.PICKING;
            case "picked" -> ShipmentStatus.PICKED;
            case "storing" -> ShipmentStatus.STORING;
            case "transporting" -> ShipmentStatus.IN_TRANSIT;
            case "delivering" -> ShipmentStatus.DELIVERING;
            case "delivered" -> ShipmentStatus.DELIVERED;
            case "delivery_fail" -> ShipmentStatus.DELIVERY_FAIL;
            case "waiting_to_return" -> ShipmentStatus.WAITING_TO_RETURN;
            case "return" -> ShipmentStatus.RETURN;
            case "returned" -> ShipmentStatus.RETURNED;
            case "cancel" -> ShipmentStatus.CANCEL;
            case "exception" -> ShipmentStatus.EXCEPTION;
            default -> ShipmentStatus.PENDING;
        };
    }
    
    private ShipmentDTO toDTO(Shipment shipment) {
        return ShipmentDTO.builder()
            .id(shipment.getId())
            .shipmentType(shipment.getShipmentType())
            .orderId(shipment.getOrderId())
            .inventoryRequestId(shipment.getInventoryRequestId())
            .carrier(shipment.getCarrier())
            .trackingNumber(shipment.getTrackingNumber())
            .carrierOrderCode(shipment.getCarrierOrderCode())
            .status(shipment.getStatus())
            .fromName(shipment.getFromName())
            .fromPhone(shipment.getFromPhone())
            .fromAddress(shipment.getFromAddress())
            .fromWardCode(shipment.getFromWardCode())
            .fromDistrictId(shipment.getFromDistrictId())
            .toName(shipment.getToName())
            .toPhone(shipment.getToPhone())
            .toAddress(shipment.getToAddress())
            .toWardCode(shipment.getToWardCode())
            .toDistrictId(shipment.getToDistrictId())
            .weight(shipment.getWeight())
            .length(shipment.getLength())
            .width(shipment.getWidth())
            .height(shipment.getHeight())
            .codAmount(shipment.getCodAmount())
            .insuranceValue(shipment.getInsuranceValue())
            .shippingFee(shipment.getShippingFee())
            .serviceTypeId(shipment.getServiceTypeId())
            .paymentTypeId(shipment.getPaymentTypeId())
            .note(shipment.getNote())
            .requiredNote(shipment.getRequiredNote())
            .expectedDeliveryTime(shipment.getExpectedDeliveryTime())
            .pickedAt(shipment.getPickedAt())
            .deliveredAt(shipment.getDeliveredAt())
            .createdAt(shipment.getCreatedAt())
            .fromStoreId(shipment.getFromStoreId())
            .build();
    }
}
