package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.AccessoryCompatibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessoryCompatibilityDTO {

    private Long id;
    private Long accessoryProductId;
    private String accessoryProductName;
    private Integer compatibleFanTypeId;
    private String compatibleFanTypeName;
    private Integer compatibleBrandId;
    private String compatibleBrandName;
    private String compatibleModel;
    private String notes;
    private LocalDateTime createdAt;
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

    public boolean hasFanTypeCompatibility() {
        return compatibleFanTypeId != null;
    }

    public boolean hasBrandCompatibility() {
        return compatibleBrandId != null;
    }

    public boolean hasModelCompatibility() {
        return compatibleModel != null && !compatibleModel.trim().isEmpty();
    }

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
