package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    CartService cartService;

    @PostMapping("/addToCart")
    public ApiResponse<Void> addToCart(@RequestParam int productID,@RequestParam String userName) {
        boolean status = cartService.addToCart(productID, userName);
        if (status) {
            return ApiResponse.<Void>builder().message("Thêm vào giỏ hàng thành công").build();
        }else return ApiResponse.<Void>builder().statusCode(201).message("Có lỗi khi thêm vào giỏ hàng").build();

    }
}
