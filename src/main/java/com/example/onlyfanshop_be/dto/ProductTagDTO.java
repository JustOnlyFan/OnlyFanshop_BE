package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.ProductTag;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductTagDTO {
    private Long id;
    private Long productId;
    private Integer tagId;
    private String tagCode;
    private String tagDisplayName;
    private String tagBadgeColor;
    private TagDTO tag;
    
    public static ProductTagDTO fromEntity(ProductTag entity) {
        if (entity == null) {
            return null;
        }
        
        ProductTagDTOBuilder builder = ProductTagDTO.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .tagId(entity.getTagId());
        
        if (entity.getTag() != null) {
            builder.tagCode(entity.getTag().getCode())
                   .tagDisplayName(entity.getTag().getDisplayName())
                   .tagBadgeColor(entity.getTag().getBadgeColor())
                   .tag(TagDTO.fromEntity(entity.getTag()));
        }
        
        return builder.build();
    }
}
