package com.example.onlyfanshop_be.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectTransferRequestDTO {

    private String reason;
}
