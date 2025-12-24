package com.example.onlyfanshop_be.scheduler;

import com.example.onlyfanshop_be.dto.ghn.GHNValidationResult;
import com.example.onlyfanshop_be.service.IGHNConfigService;
import com.example.onlyfanshop_be.service.IInventoryNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * GHNTokenCheckScheduler - Periodically validates GHN API token
 * Requirements: 10.5 - WHEN GHN token expires THEN the System SHALL notify Admin to update the configuration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GHNTokenCheckScheduler {
    
    private final IGHNConfigService ghnConfigService;
    private final IInventoryNotificationService notificationService;
    
    // Track consecutive failures to avoid spamming notifications
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES_BEFORE_NOTIFY = 3;
    
    /**
     * Validate GHN token every hour
     * Requirements: 10.5 - Notify Admin when GHN token expires
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3,600,000 ms
    public void validateGHNToken() {
        log.info("Starting scheduled GHN token validation...");
        
        try {
            // Check if there's an active configuration
            if (!ghnConfigService.hasActiveConfiguration()) {
                log.warn("No active GHN configuration found");
                return;
            }
            
            // Validate the current configuration
            GHNValidationResult result = ghnConfigService.validateCurrentConfiguration();
            
            if (result.isValid()) {
                log.info("GHN token validation successful");
                consecutiveFailures = 0; // Reset failure counter
            } else {
                consecutiveFailures++;
                log.warn("GHN token validation failed (attempt {}): {}", 
                        consecutiveFailures, result.getMessage());
                
                // Notify admin after consecutive failures
                if (consecutiveFailures >= MAX_FAILURES_BEFORE_NOTIFY) {
                    notificationService.notifyGHNTokenExpiry(0); // 0 indicates expired/invalid
                    log.error("GHN token appears to be expired or invalid. Admin notified.");
                }
            }
            
        } catch (Exception e) {
            consecutiveFailures++;
            log.error("Error during GHN token validation: {}", e.getMessage(), e);
            
            if (consecutiveFailures >= MAX_FAILURES_BEFORE_NOTIFY) {
                notificationService.notifyGHNApiError(
                        "Token validation failed: " + e.getMessage(), 
                        null
                );
            }
        }
    }
    
    /**
     * Daily check at 8 AM to remind about token status
     * This provides a daily summary notification if there are issues
     */
    @Scheduled(cron = "0 0 8 * * ?") // Every day at 8:00 AM
    public void dailyTokenCheck() {
        log.info("Running daily GHN token check...");
        
        try {
            if (!ghnConfigService.hasActiveConfiguration()) {
                notificationService.notifyGHNApiError(
                        "Chưa có cấu hình GHN. Vui lòng thiết lập cấu hình GHN để sử dụng dịch vụ vận chuyển.",
                        null
                );
                return;
            }
            
            GHNValidationResult result = ghnConfigService.validateCurrentConfiguration();
            
            if (!result.isValid()) {
                notificationService.notifyGHNTokenExpiry(0);
            }
            
        } catch (Exception e) {
            log.error("Error during daily GHN token check: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Initial validation on application startup (with delay)
     * Runs 2 minutes after application starts
     */
    @Scheduled(initialDelay = 120000, fixedDelay = Long.MAX_VALUE)
    public void initialValidation() {
        log.info("Running initial GHN token validation on startup...");
        validateGHNToken();
    }
}
