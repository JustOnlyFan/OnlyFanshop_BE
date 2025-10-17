package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    CartService cartService;
    @Autowired
    UserRepository userRepository;

    @PostMapping("/addToCart")
    public ApiResponse<Void> addToCart(@RequestParam int productID,@RequestParam String username) {
        boolean status = cartService.addToCart(productID, username);
        if (status) {
            return ApiResponse.<Void>builder().message("Thêm vào giỏ hàng thành công").statusCode(200).build();
        }else return ApiResponse.<Void>builder().statusCode(201).message("Có lỗi khi thêm vào giỏ hàng").build();

    }

    @PostMapping("/clear")
    public ApiResponse<Void> clearCart(@RequestParam String username) {
        boolean checkUser = userRepository.findByUsername(username).isPresent();
        if (checkUser) {
            cartService.clearCart(username);
            return ApiResponse.<Void>builder().message("Đã xóa toàn bộ").statusCode(200).build();
        }else return ApiResponse.<Void>builder().statusCode(201).message("Có lỗi khi xóa").build();

    }

    @GetMapping("/{userId}")
    public ApiResponse<CartDTO> getCart(@PathVariable int userId) {
        return cartService.getCart(userId);
    }

}
