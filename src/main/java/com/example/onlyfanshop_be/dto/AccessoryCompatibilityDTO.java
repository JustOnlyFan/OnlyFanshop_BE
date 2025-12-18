package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.AccessoryCompatibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for AccessoryCompatibility entity.
 * Used for displaying compatibility information between accessory products and fan types/brands/models.
 * 
 * Requirements: 8.3 - Display which fan types and models the accessory is compatible with
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessoryCompatibilityDTO {
    
    /**
     * Unique identifier for the compatibility entry.
     */
    private Long id;
    
    /**
     * The accessory product ID that this compatibility entry belongs to.
     */
    private Long accessoryProductId;
    
    /**
     * The accessory product name (for display purposes).
     */
    private String accessoryProductName;
    
    /**
     * The fan type category ID that this accessory is compatible with.
     */
    private Integer compatibleFanTypeId;
    
    /**
     * The fan type category name (for display purposes).
     */
    private String compatibleFanTypeName;
    
    /**
     * The brand ID that this accessory is compatible with.
     */
    private Integer compatibleBrandId;
    
    /**
     * The brand name (for display purposes).
     */
    private String compatibleBrandName;
    
    /**
     * Specific model names that this accessory is compatible with.
     */
    private String compatibleModel;
    
    /**
     * Additional notes about compatibility.
     */
    private String notes;
    
    /**
     * Timestamp when this compatibility entry was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Converts an AccessoryCompatibility entity to AccessoryCompatibilityDTO.
     * Resolves related entity names (fan type, brand, product) for display.
     * 
     * @param entity the AccessoryCompatibility entity
     * @return AccessoryCompatibilityDTO representation
     */
    public static AccessoryCompatibilityDTO fromEntity(AccessoryCompatibility entity) {
        if (entity == null) {
            return null;
        }
        
        return AccessoryCompatibilityDTO.builder()
                .id(entity.getId())
                .accessoryProductId(entity.getAccessoryProductId())
                .accessoryProductName(entity.getAccessoryProduct() != null 
                        ? entity.getAccessoryProduct().getName() : null)
                .compatibleFanTypeId(entity.getCompatibleFanTypeId())
                .compatibleFanTypeName(entity.getCompatibleFanType() != null 
                        ? entity.getCompatibleFanType().getName() : null)
                .compatibleBrandId(entity.getCompatibleBrandId())
                .compatibleBrandName(entity.getCompatibleBrand() != null 
                        ? entity.getCompatibleBrand().getName() : null)
                .compatibleModel(entity.getCompatibleModel())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    /**
     * Converts this DTO to an AccessoryCompatibility entity.
     * Note: This does not set the related entity references (accessoryProduct, compatibleFanType, compatibleBrand).
     * Only the ID fields are set.
     * 
     * @return AccessoryCompatibility entity
     */
    public AccessoryCompatibility toEntity() {
        return AccessoryCompatibility.builder()
                .id(this.id)
                .accessoryProductId(this.accessoryProductId)
                .compatibleFanTypeId(this.compatibleFanTypeId)
                .compatibleBrandId(this.compatibleBrandId)
                .compatibleModel(this.compatibleModel)
                .notes(this.notes)
                .createdAt(this.createdAt)
                .build();
    }
    
    /**
     * Creates a simple AccessoryCompatibilityDTO with only essential fields.
     * Useful for creating new compatibility entries.
     * 
     * @param accessoryProductId the accessory product ID
     * @param compatibleFanTypeId the compatible fan type category ID
     * @param compatibleBrandId the compatible brand ID
     * @param compatibleModel the compatible model names
     * @return simple AccessoryCompatibilityDTO
     */
    public static AccessoryCompatibilityDTO simple(
            Long accessoryProductId,
            Integer compatibleFanTypeId,
            Integer compatibleBrandId,
            String compatibleModel) {
        return AccessoryCompatibilityDTO.builder()
                .accessoryProductId(accessoryProductId)
                .compatibleFanTypeId(compatibleFanTypeId)
                .compatibleBrandId(compatibleBrandId)
                .compatibleModel(compatibleModel)
                .build();
    }
    
    /**
     * Checks if this compatibility entry has fan type information.
     * 
     * @return true if fan type ID is set
     */
    public boolean hasFanTypeCompatibility() {
        return compatibleFanTypeId != null;
    }
    
    /**
     * Checks if this compatibility entry has brand information.
     * 
     * @return true if brand ID is set
     */
    public boolean hasBrandCompatibility() {
        return compatibleBrandId != null;
    }
    
    /**
     * Checks if this compatibility entry has model information.
     * 
     * @return true if model string is not null or empty
     */
    public boolean hasModelCompatibility() {
        return compatibleModel != null && !compatibleModel.trim().isEmpty();
    }
    
    /**
     * Gets a display-friendly summary of the compatibility.
     * 
     * @return formatted compatibility summary string
     */
    public String getCompatibilitySummary() {
        StringBuilder summary = new StringBuilder();
        
        if (compatibleFanTypeName != null) {
            summary.append(compatibleFanTypeName);
        }
        
        if (compatibleBrandName != null) {
            if (summary.length() > 0) {
                summary.append(" - ");
            }
            summary.append(compatibleBrandName);
        }
        
        if (compatibleModel != null && !compatibleModel.trim().isEmpty()) {
            if (summary.length() > 0) {
                summary.append(" (");
                summary.append(compatibleModel);
                summary.append(")");
            } else {
                summary.append(compatibleModel);
            }
        }
        
        return summary.length() > 0 ? summary.toString() : "Universal";
    }
}
