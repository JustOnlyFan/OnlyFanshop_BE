package com.example.onlyfanshop_be.scheduler;

import com.example.onlyfanshop_be.dto.DebtOrderDTO;
import com.example.onlyfanshop_be.service.IDebtOrderService;
import com.example.onlyfanshop_be.service.IInventoryNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DebtOrderCheckScheduler - Periodically checks for fulfillable debt orders
 * Requirements: 6.3 - WHEN Admin updates Main_Warehouse inventory THEN the System SHALL check if any Debt_Orders can be fulfilled
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DebtOrderCheckScheduler {
    
    private final IDebtOrderService debtOrderService;
    private final IInventoryNotificationService notificationService;
    
    /**
     * Check for fulfillable debt orders every 10 minutes
     * Requirements: 6.3 - Check if any Debt_Orders can be fulfilled when inventory changes
     * Requirements: 6.4 - Notify Admin when a Debt_Order can be fulfilled
     */
    @Scheduled(fixedRate = 600000) // 10 minutes = 600,000 ms
    public void checkFulfillableDebtOrders() {
        log.info("Starting scheduled debt order fulfillment check...");
        
        try {
            // Check and update fulfillable debt orders
            List<DebtOrderDTO> fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
            
            if (fulfillableOrders.isEmpty()) {
                log.debug("No debt orders became fulfillable");
                return;
            }
            
            log.info("Found {} debt orders that are now fulfillable", fulfillableOrders.size());
            
            // Send notifications for each fulfillable debt order
            for (DebtOrderDTO debtOrder : fulfillableOrders) {
                try {
                    notificationService.notifyDebtOrderFulfillable(
                            debtOrder.getId(),
                            debtOrder.getTransferRequestId()
                    );
                } catch (Exception e) {
                    log.error("Error sending notification for debt order {}: {}", 
                            debtOrder.getId(), e.getMessage());
                }
            }
            
            log.info("Debt order fulfillment check completed: {} orders now fulfillable", 
                    fulfillableOrders.size());
            
        } catch (Exception e) {
            log.error("Error during scheduled debt order check: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Initial check on application startup (with delay)
     * Runs 60 seconds after application starts
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void initialCheck() {
        log.info("Running initial debt order fulfillment check on startup...");
        checkFulfillableDebtOrders();
    }
}
