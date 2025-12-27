package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequestDTO {

    @NotNull(message = "Source warehouse ID is required")
    private Long sourceWarehouseId;

    @NotEmpty(message = "Items cannot be empty")
    private List<CreateTransferRequestItemDTO> items;

    private String note;
}
