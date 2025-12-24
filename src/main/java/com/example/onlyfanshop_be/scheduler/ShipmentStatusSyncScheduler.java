package com.example.onlyfanshop_be.scheduler;

import com.example.onlyfanshop_be.entity.InternalShipment;
import com.example.onlyfanshop_be.enums.InternalShipmentStatus;
import com.example.onlyfanshop_be.service.IInternalShipmentService;
import com.example.onlyfanshop_be.service.IInventoryNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ShipmentStatusSyncScheduler - Periodically syncs shipment status from GHN API
 * Requirements: 8.1 - WHEN a Shipment exists THEN the System SHALL periodically poll GHN_API for status updates
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShipmentStatusSyncScheduler {
    
    private final IInternalShipmentService internalShipmentService;
    private final IInventoryNotificationService notificationService;
    
    /**
     * Sync shipment status from GHN API every 5 minutes
     * Requirements: 8.1 - Periodically poll GHN_API for status updates
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 ms
    public void syncShipmentStatuses() {
        log.info("Starting scheduled shipment status sync...");
        
        try {
            // Get all in-transit shipments
            List<InternalShipment> inTransitShipments = internalShipmentService.getInTransitShipments();
            
            if (inTransitShipments.isEmpty()) {
                log.debug("No in-transit shipments to sync");
                return;
            }
            
            log.info("Found {} in-transit shipments to sync", inTransitShipments.size());
            
            int syncedCount = 0;
            int errorCount = 0;
            
            for (InternalShipment shipment : inTransitShipments) {
                try {
                    if (shipment.getGhnOrderCode() != null) {
                        InternalShipmentStatus oldStatus = shipment.getStatus();
                        IInternalShipmentService.InternalShipmentDTO updatedShipment = 
                                internalShipmentService.syncShipmentStatus(shipment.getId());
                        
                        // Check if status changed and notify
                        if (updatedShipment.status() != oldStatus) {
                            notificationService.notifyShipmentStatusChange(
                                    shipment.getId(),
                                    oldStatus,
                                    updatedShipment.status()
                            );
                        }
                        
                        syncedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error syncing shipment {}: {}", shipment.getId(), e.getMessage());
                    errorCount++;
                }
            }
            
            log.info("Shipment status sync completed: {} synced, {} errors", syncedCount, errorCount);
            
        } catch (Exception e) {
            log.error("Error during scheduled shipment status sync: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Initial sync on application startup (with delay)
     * Runs 30 seconds after application starts
     */
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)
    public void initialSync() {
        log.info("Running initial shipment status sync on startup...");
        syncShipmentStatuses();
    }
}
