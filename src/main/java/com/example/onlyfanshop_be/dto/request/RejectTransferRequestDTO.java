package com.example.onlyfanshop_be.dto.request;

import lombok.*;

/**
 * DTO for rejecting a Transfer Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectTransferRequestDTO {
    /**
     * The reason for rejection
     */
    private String reason;
}
