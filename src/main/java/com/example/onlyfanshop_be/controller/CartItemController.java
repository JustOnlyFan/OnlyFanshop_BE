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
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
        List<CartItem> cartItem = cartItemRepository.findByCart_CartID(cart.getFirst().getCartID());
        if (cartItem.isEmpty()) {
            response.setData(Collections.emptyList());
            response.setMessage("No cart found");
            return response;
        }
        response.setData(cartItem);

        return  response;
    }
    @PostMapping("/addQuantity")
    public ApiResponse<Void> addQuantity(@RequestParam String username, @RequestParam Integer productID){
        ApiResponse<Void> response = new ApiResponse<>();
        boolean existUser = userRepository.existsByUsername(username);
        if(!existUser){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        Cart cart = cartRepository.findByStatusAndUser_username("Inprogress", username).getFirst();
        CartItem cartItem = cartItemRepository.findByCart_CartIDAndProduct_ProductID(cart.getCartID(), productID);
        cartItem.setQuantity(cartItem.getQuantity() + 1);
        cartItem.setPrice(cartItem.getPrice() + cartItem.getProduct().getPrice());
        cart.setTotalPrice(cart.getTotalPrice() + cartItem.getProduct().getPrice());
        cartRepository.save(cart);
        cartItemRepository.save(cartItem);
        return   response;
    }

    @PostMapping("/minusQuantity")
    public ApiResponse<Void> minusQuantity(@RequestParam String username, @RequestParam Integer productID){
        ApiResponse<Void> response = new ApiResponse<>();
        boolean existUser = userRepository.existsByUsername(username);
        if(!existUser){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        Cart cart = cartRepository.findByStatusAndUser_username("Inprogress", username).getFirst();
        CartItem cartItem = cartItemRepository.findByCart_CartIDAndProduct_ProductID(cart.getCartID(), productID);
        cartItem.setQuantity(cartItem.getQuantity()-1);
        cart.setTotalPrice(cart.getTotalPrice() - cartItem.getProduct().getPrice());
        cartRepository.save(cart);
        if(cartItem.getQuantity() == 0){
            cartItemRepository.delete(cartItem);
        }else {
            cartItem.setPrice(cartItem.getPrice() - cartItem.getProduct().getPrice());
            cartItemRepository.save(cartItem);
        }
        return response;
    }
}
