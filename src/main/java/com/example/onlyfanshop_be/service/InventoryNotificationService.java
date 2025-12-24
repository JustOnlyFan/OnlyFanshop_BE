package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.InternalShipmentStatus;
import com.example.onlyfanshop_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * InventoryNotificationService - Implementation for inventory-related notifications
 * Handles notifications for shipment status changes, debt order fulfillment, and GHN token expiry
 * Requirements: 8.5, 6.4, 10.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryNotificationService implements IInventoryNotificationService {
    
    private final NotificationService notificationService;
    private final InternalShipmentRepository internalShipmentRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final UserRepository userRepository;
    private final StoreLocationRepository storeLocationRepository;
    
    // Role IDs - typically ADMIN=1, STAFF=2, USER=3
    private static final byte ADMIN_ROLE_ID = 1;
    private static final byte STAFF_ROLE_ID = 2;
    
    /**
     * Notify staff when shipment status changes
     * Requirements: 8.5 - WHEN Shipment status changes THEN the System SHALL send notification to relevant Staff
     */
    @Override
    public void notifyShipmentStatusChange(Long shipmentId, InternalShipmentStatus oldStatus, InternalShipmentStatus newStatus) {
        try {
            Optional<InternalShipment> shipmentOpt = internalShipmentRepository.findById(shipmentId);
            if (shipmentOpt.isEmpty()) {
                log.warn("Cannot notify: Shipment {} not found", shipmentId);
                return;
            }
            
            InternalShipment shipment = shipmentOpt.get();
            
            // Get the transfer request to find the store
            Optional<TransferRequest> requestOpt = transferRequestRepository.findById(shipment.getTransferRequestId());
            if (requestOpt.isEmpty()) {
                log.warn("Cannot notify: Transfer request {} not found", shipment.getTransferRequestId());
                return;
            }
            
            TransferRequest request = requestOpt.get();
            Integer storeId = request.getStoreId();
            
            // Build notification message
            String statusMessage = getStatusMessage(newStatus);
            String message = String.format(
                    "Đơn vận chuyển #%d %s. Mã GHN: %s",
                    shipmentId,
                    statusMessage,
                    shipment.getGhnOrderCode() != null ? shipment.getGhnOrderCode() : "N/A"
            );
            
            // Notify staff of the store
            notifyStoreStaff(storeId, message);
            
            // If delivered, add special notification
            if (newStatus == InternalShipmentStatus.DELIVERED) {
                String deliveredMessage = String.format(
                        "Đơn vận chuyển #%d đã giao thành công! Hàng đã được cập nhật vào kho cửa hàng.",
                        shipmentId
                );
                notifyStoreStaff(storeId, deliveredMessage);
            }
            
            log.info("Sent shipment status notification for shipment {} to store {}", shipmentId, storeId);
            
        } catch (Exception e) {
            log.error("Error sending shipment status notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notify admin when a debt order becomes fulfillable
     * Requirements: 6.4 - WHEN a Debt_Order can be fulfilled THEN the System SHALL notify Admin
     */
    @Override
    public void notifyDebtOrderFulfillable(Long debtOrderId, Long transferRequestId) {
        try {
            String message = String.format(
                    "Đơn nợ #%d (từ yêu cầu chuyển kho #%d) có thể được đáp ứng. Vui lòng kiểm tra và xử lý.",
                    debtOrderId,
                    transferRequestId
            );
            
            notifyAllAdmins(message);
            
            log.info("Sent debt order fulfillable notification for debt order {}", debtOrderId);
            
        } catch (Exception e) {
            log.error("Error sending debt order notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notify admin when GHN token is about to expire
     * Requirements: 10.5 - WHEN GHN token expires THEN the System SHALL notify Admin to update the configuration
     */
    @Override
    public void notifyGHNTokenExpiry(int daysUntilExpiry) {
        try {
            String message;
            if (daysUntilExpiry <= 0) {
                message = "⚠️ CẢNH BÁO: Token GHN đã hết hạn! Vui lòng cập nhật cấu hình GHN ngay để tiếp tục sử dụng dịch vụ vận chuyển.";
            } else if (daysUntilExpiry <= 7) {
                message = String.format(
                        "⚠️ Token GHN sẽ hết hạn trong %d ngày. Vui lòng cập nhật cấu hình GHN sớm.",
                        daysUntilExpiry
                );
            } else {
                message = String.format(
                        "Token GHN sẽ hết hạn trong %d ngày. Hãy lên kế hoạch cập nhật cấu hình.",
                        daysUntilExpiry
                );
            }
            
            notifyAllAdmins(message);
            
            log.info("Sent GHN token expiry notification: {} days until expiry", daysUntilExpiry);
            
        } catch (Exception e) {
            log.error("Error sending GHN token expiry notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Notify admin about GHN API errors
     * Requirements: 7.4 - WHEN GHN_API returns error THEN the System SHALL notify Admin with error details
     */
    @Override
    public void notifyGHNApiError(String errorMessage, Long shipmentId) {
        try {
            String message;
            if (shipmentId != null) {
                message = String.format(
                        "❌ Lỗi GHN API cho đơn vận chuyển #%d: %s",
                        shipmentId,
                        errorMessage
                );
            } else {
                message = String.format("❌ Lỗi GHN API: %s", errorMessage);
            }
            
            notifyAllAdmins(message);
            
            log.info("Sent GHN API error notification: {}", errorMessage);
            
        } catch (Exception e) {
            log.error("Error sending GHN API error notification: {}", e.getMessage(), e);
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    /**
     * Get human-readable status message
     */
    private String getStatusMessage(InternalShipmentStatus status) {
        return switch (status) {
            case CREATED -> "đã được tạo";
            case PICKING -> "đang được lấy hàng";
            case PICKED -> "đã lấy hàng xong";
            case IN_TRANSIT -> "đang vận chuyển";
            case DELIVERING -> "đang giao hàng";
            case DELIVERED -> "đã giao thành công";
            case CANCELLED -> "đã bị hủy";
            case RETURN -> "đang hoàn trả";
        };
    }
    
    /**
     * Notify all staff members of a specific store
     */
    private void notifyStoreStaff(Integer storeId, String message) {
        if (storeId == null) {
            log.warn("Cannot notify store staff: storeId is null");
            return;
        }
        
        // Find staff users associated with this store
        List<User> storeStaff = userRepository.findByStoreLocationId(storeId);
        
        for (User staff : storeStaff) {
            try {
                notificationService.sendNotification(staff.getId().intValue(), message);
            } catch (Exception e) {
                log.error("Error sending notification to staff {}: {}", staff.getId(), e.getMessage());
            }
        }
        
        // Also notify admins
        notifyAllAdmins(message);
    }
    
    /**
     * Notify all admin users
     */
    private void notifyAllAdmins(String message) {
        List<User> admins = userRepository.findByRoleId(ADMIN_ROLE_ID);
        
        for (User admin : admins) {
            try {
                notificationService.sendNotification(admin.getId().intValue(), message);
            } catch (Exception e) {
                log.error("Error sending notification to admin {}: {}", admin.getId(), e.getMessage());
            }
        }
    }
}
