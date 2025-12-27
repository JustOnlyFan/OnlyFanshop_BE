package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryNotificationService implements IInventoryNotificationService {
    
    private final NotificationService notificationService;
    private final TransferRequestRepository transferRequestRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    private static final byte ADMIN_ROLE_ID = 1;

    @Override
    public void notifyTransferRequestStatusChange(Long requestId, String oldStatus, String newStatus) {
        try {
            Optional<TransferRequest> requestOpt = transferRequestRepository.findById(requestId);
            if (requestOpt.isEmpty()) {
                log.warn("Cannot notify: Transfer request {} not found", requestId);
                return;
            }
            
            TransferRequest request = requestOpt.get();
            Integer storeId = request.getStoreId();

            String message = String.format(
                    "Yêu cầu chuyển kho #%d đã chuyển trạng thái từ %s sang %s",
                    requestId,
                    oldStatus,
                    newStatus
            );

            notifyStoreStaff(storeId, message);
            
            log.info("Sent transfer request status notification for request {} to store {}", requestId, storeId);
            
        } catch (Exception e) {
            log.error("Error sending transfer request status notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void notifyLowStock(Long productId, Integer storeId, Integer currentQuantity) {
        try {
            String message = String.format(
                    "⚠️ Cảnh báo: Sản phẩm #%d tại cửa hàng #%d còn %d sản phẩm. Vui lòng bổ sung hàng.",
                    productId,
                    storeId,
                    currentQuantity
            );
            
            notifyStoreStaff(storeId, message);
            notifyAllAdmins(message);
            
            log.info("Sent low stock notification for product {} at store {}", productId, storeId);
            
        } catch (Exception e) {
            log.error("Error sending low stock notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void notifyInventoryUpdate(Long warehouseId, Long productId, Integer previousQuantity, Integer newQuantity) {
        try {
            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
            String warehouseName = warehouseOpt.map(Warehouse::getName).orElse("Unknown");
            
            String message = String.format(
                    "Tồn kho sản phẩm #%d tại kho %s đã được cập nhật: %d → %d",
                    productId,
                    warehouseName,
                    previousQuantity,
                    newQuantity
            );
            
            notifyAllAdmins(message);
            
            log.info("Sent inventory update notification for product {} at warehouse {}", productId, warehouseId);
            
        } catch (Exception e) {
            log.error("Error sending inventory update notification: {}", e.getMessage(), e);
        }
    }

    private void notifyStoreStaff(Integer storeId, String message) {
        if (storeId == null) {
            log.warn("Cannot notify store staff: storeId is null");
            return;
        }

        List<User> storeStaff = userRepository.findByStoreLocationId(storeId);
        
        for (User staff : storeStaff) {
            try {
                notificationService.sendNotification(staff.getId().intValue(), message);
            } catch (Exception e) {
                log.error("Error sending notification to staff {}: {}", staff.getId(), e.getMessage());
            }
        }
        notifyAllAdmins(message);
    }

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
