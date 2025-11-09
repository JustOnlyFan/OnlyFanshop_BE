package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.request.AddToCartRequest;
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
    public ApiResponse<Void> addToCart(@RequestBody AddToCartRequest request) {
        boolean status = cartService.addToCart(request);
        if (status) {
            return ApiResponse.<Void>builder().message("Thêm vào giỏ hàng thành công").statusCode(200).build();
        }else return ApiResponse.<Void>builder().statusCode(201).message("Có lỗi khi thêm vào giỏ hàng").build();

    }

    @PostMapping("/clear")
    public ApiResponse<Void> clearCart(@RequestParam String username) {
        // Try to find user by email first, if not found, try as userId
        boolean checkUser = userRepository.findByEmail(username).isPresent() ||
                (username.matches("\\d+") && userRepository.findById(Long.parseLong(username)).isPresent());
        if (checkUser) {
            cartService.clearCart(username);
            return ApiResponse.<Void>builder().message("Đã xóa toàn bộ").statusCode(200).build();
        }else return ApiResponse.<Void>builder().statusCode(201).message("Có lỗi khi xóa").build();

    }

    @PostMapping("/instantBuy")
    public ApiResponse<Void> instantBuy(@RequestBody AddToCartRequest request) {
        ApiResponse<Void> respone = new ApiResponse<>();
        Cart cart = cartService.instantBuy(request);
        if(cart!=null) {
            respone.setMessage("Tạo giỏ hàng thanh toán thành công");
        }else{ respone.setMessage("Có lỗi khi tạo giỏ hàng thanh toán");
                respone.setStatusCode(201);
        }
        return respone;
    }

    @GetMapping("/{userId}")
    public ApiResponse<CartDTO> getCart(@PathVariable int userId) {
        return cartService.getCart(userId,"InProgress");
    }

    @DeleteMapping("/deleteInstantCart")
    public ApiResponse<Void> deleteInstantCart(@RequestParam Integer userID) {
        ApiResponse<Void> respone = new ApiResponse<>();
        cartService.deleteInstantCart(userID);
        return respone;
    }

}
