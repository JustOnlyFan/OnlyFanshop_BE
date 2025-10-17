package com.example.onlyfanshop_be.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddToCartRequest {
    Integer productId;
    Integer quantity;
    String userName;

}
