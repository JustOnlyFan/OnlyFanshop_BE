package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveInventoryRequestDTO {
    @NotNull(message = "Số lượng duyệt không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer approvedQuantity;

    private String adminNote;
}
