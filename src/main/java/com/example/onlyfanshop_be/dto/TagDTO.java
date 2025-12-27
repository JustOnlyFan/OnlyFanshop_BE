package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Tag;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagDTO {
    private Integer id;
    private String code;
    private String displayName;
    private String badgeColor;
    private Integer displayOrder;

    public static TagDTO fromEntity(Tag tag) {
        if (tag == null) {
            return null;
        }
        return TagDTO.builder()
                .id(tag.getId())
                .code(tag.getCode())
                .displayName(tag.getDisplayName())
                .badgeColor(tag.getBadgeColor())
                .displayOrder(tag.getDisplayOrder())
                .build();
    }

    public Tag toEntity() {
        return Tag.builder()
                .id(this.id)
                .code(this.code)
                .displayName(this.displayName)
                .badgeColor(this.badgeColor)
                .displayOrder(this.displayOrder)
                .build();
    }
}
