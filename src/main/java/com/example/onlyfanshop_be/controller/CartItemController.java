package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cartItem")
public class CartItemController {
    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    UserRepository userRepository;

    @GetMapping("/showCartItem")
    public ApiResponse<List<CartItem>> showCartItem(@RequestParam String username){
        boolean existUser = userRepository.existsByUsername(username);
        if(!existUser){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        ApiResponse<List<CartItem>> response = new ApiResponse<>();
        List<Cart> cart = cartRepository.findByStatusAndUser_username("Inprogress", username);
        if(cart.isEmpty()){
            throw new AppException(ErrorCode.CARTITEM_NOTHING);
        }
        List<CartItem> cartItem = cartItemRepository.findByCart_CartID(cart.getFirst().getCartID());
        if(cartItem.isEmpty()){
            throw new AppException(ErrorCode.CARTITEM_NOTHING);
        }
        response.setData(cartItem);

        return  response;
    }
}
