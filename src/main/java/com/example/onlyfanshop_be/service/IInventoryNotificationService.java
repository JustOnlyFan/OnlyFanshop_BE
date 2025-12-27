package com.example.onlyfanshop_be.service;

public interface IInventoryNotificationService {

    void notifyTransferRequestStatusChange(Long requestId, String oldStatus, String newStatus);

    void notifyLowStock(Long productId, Integer storeId, Integer currentQuantity);

    void notifyInventoryUpdate(Long warehouseId, Long productId, Integer previousQuantity, Integer newQuantity);
}
