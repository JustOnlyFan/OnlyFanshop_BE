package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ghn.GHNConfigDTO;
import com.example.onlyfanshop_be.dto.ghn.GHNConfigUpdateRequest;
import com.example.onlyfanshop_be.dto.ghn.GHNValidationResult;
import com.example.onlyfanshop_be.entity.GHNConfiguration;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.GHNConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * GHNConfigService - Implementation của IGHNConfigService
 * Quản lý cấu hình GHN API
 */
@Service
@Slf4j
public class GHNConfigService implements IGHNConfigService {
    
    private final GHNConfigurationRepository ghnConfigurationRepository;
    private final IGHNService ghnService;
    
    public GHNConfigService(GHNConfigurationRepository ghnConfigurationRepository, 
                            @Lazy IGHNService ghnService) {
        this.ghnConfigurationRepository = ghnConfigurationRepository;
        this.ghnService = ghnService;
    }
    
    @Override
    public GHNConfigDTO getConfiguration() {
        GHNConfiguration config = ghnConfigurationRepository.findFirstByIsActiveTrue()
            .orElse(null);
        
        if (config == null) {
            return null;
        }
        
        return toDTO(config);
    }
    
    @Override
    @Transactional
    public GHNConfigDTO updateConfiguration(GHNConfigUpdateRequest request) {
        // Validate configuration before saving
        GHNValidationResult validationResult = ghnService.validateConfiguration(
            request.getApiToken(), 
            request.getShopId()
        );
        
        if (!validationResult.isValid()) {
            log.error("GHN configuration validation failed: {}", validationResult.getMessage());
            throw new AppException(ErrorCode.GHN_INVALID_CONFIG);
        }
        
        // Deactivate all existing configurations
        ghnConfigurationRepository.findAll().forEach(config -> {
            config.setIsActive(false);
            ghnConfigurationRepository.save(config);
        });
        
        // Create or update configuration
        GHNConfiguration config = ghnConfigurationRepository.findFirstByIsActiveTrue()
            .orElse(new GHNConfiguration());
        
        config.setApiToken(request.getApiToken());
        config.setShopId(request.getShopId());
        config.setDefaultPickupTime(request.getDefaultPickupTime());
        config.setIsActive(true);
        config.setUpdatedAt(LocalDateTime.now());
        
        GHNConfiguration savedConfig = ghnConfigurationRepository.save(config);
        log.info("GHN configuration updated successfully");
        
        return toDTO(savedConfig);
    }
    
    @Override
    public GHNValidationResult validateCurrentConfiguration() {
        if (!hasActiveConfiguration()) {
            return GHNValidationResult.failure("No active GHN configuration found");
        }
        
        return ghnService.validateConfiguration();
    }
    
    @Override
    public GHNValidationResult validateConfiguration(String apiToken, String shopId) {
        return ghnService.validateConfiguration(apiToken, shopId);
    }
    
    @Override
    public boolean hasActiveConfiguration() {
        return ghnConfigurationRepository.existsByIsActiveTrue();
    }
    
    /**
     * Convert entity to DTO
     */
    private GHNConfigDTO toDTO(GHNConfiguration config) {
        return GHNConfigDTO.builder()
            .id(config.getId())
            .apiToken(config.getApiToken())
            .shopId(config.getShopId())
            .defaultPickupTime(config.getDefaultPickupTime())
            .isActive(config.getIsActive())
            .updatedAt(config.getUpdatedAt())
            .build();
    }
}
