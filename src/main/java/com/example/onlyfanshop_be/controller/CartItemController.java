package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        // Try to find user by email first, if not found, try as userId
        User user = null;
        if (username != null) {
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty() && username.matches("\\d+")) {
                // Try as userId if it's a number
                userOpt = userRepository.findById(Long.parseLong(username));
            }
            user = userOpt.orElse(null);
        }
        
        if(user == null){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        
        ApiResponse<List<CartItem>> response = new ApiResponse<>();
        // Note: Status field removed from Cart, just get user's cart
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        Cart cart;
        if(cartOpt.isEmpty()){
            cart = Cart.builder()
                    .userId(user.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            cartRepository.save(cart);
        }else {
            cart = cartOpt.get();
        }
        List<CartItem> cartItem = cartItemRepository.findByCartId(cart.getId());
        if (cartItem.isEmpty()) {
            response.setData(Collections.emptyList());
            response.setMessage("No item found");
            return response;
        }
        response.setData(cartItem);

        return  response;
    }
    @GetMapping("/showInstantBuyItem")
        public ApiResponse<List<CartItem>> showInstantBuyItem(@RequestParam String username){
        // Try to find user by email first, if not found, try as userId
        User user = null;
        if (username != null) {
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty() && username.matches("\\d+")) {
                // Try as userId if it's a number
                userOpt = userRepository.findById(Long.parseLong(username));
            }
            user = userOpt.orElse(null);
        }
        
        if(user == null){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        
        ApiResponse<List<CartItem>> response = new ApiResponse<>();
        // Note: Status field removed from Cart, just get user's cart
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isEmpty()) {
            response.setData(Collections.emptyList());
            response.setMessage("No items found");
            return response;
        }
        Cart cart = cartOpt.get();
        List<CartItem> cartItem = cartItemRepository.findByCartId(cart.getId());
        if (cartItem.isEmpty()) {
            response.setData(Collections.emptyList());
            response.setMessage("No items found");
            return response;
        }
        response.setData(cartItem);

        return  response;
        }
    @PostMapping("/addQuantity")
    public ApiResponse<Void> addQuantity(@RequestParam String username, @RequestParam Integer productID){
        ApiResponse<Void> response = new ApiResponse<>();
        // Try to find user by email first, if not found, try as userId
        User user = null;
        if (username != null) {
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty() && username.matches("\\d+")) {
                // Try as userId if it's a number
                userOpt = userRepository.findById(Long.parseLong(username));
            }
            user = userOpt.orElse(null);
        }
        
        if(user == null){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
        Optional<CartItem> cartItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), (long) productID);
        if (cartItemOpt.isPresent()) {
            CartItem cartItem = cartItemOpt.get();
            cartItem.setQuantity(cartItem.getQuantity() + 1);
            cartItem.setUpdatedAt(LocalDateTime.now());
            // Note: unitPriceSnapshot should remain the same (snapshot at add time)
            cartItemRepository.save(cartItem);
        }
        return   response;
    }

    @PostMapping("/minusQuantity")
    public ApiResponse<Void> minusQuantity(@RequestParam String username, @RequestParam Integer productID){
        ApiResponse<Void> response = new ApiResponse<>();
        // Try to find user by email first, if not found, try as userId
        User user = null;
        if (username != null) {
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty() && username.matches("\\d+")) {
                // Try as userId if it's a number
                userOpt = userRepository.findById(Long.parseLong(username));
            }
            user = userOpt.orElse(null);
        }
        
        if(user == null){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
        Optional<CartItem> cartItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), (long) productID);
        if (cartItemOpt.isPresent()) {
            CartItem cartItem = cartItemOpt.get();
            cartItem.setQuantity(cartItem.getQuantity() - 1);
            // Note: unitPriceSnapshot should remain the same (snapshot at add time)
            if(cartItem.getQuantity() <= 0){
                cartItemRepository.delete(cartItem);
            }else {
                cartItem.setUpdatedAt(LocalDateTime.now());
                cartItemRepository.save(cartItem);
            }
        }
        return response;
    }

    @PutMapping("/onCheck")
    public ApiResponse<Void> onCheck(@RequestParam Integer cartItemID){
        ApiResponse<Void> response = new ApiResponse<>();
        CartItem item = cartItemRepository.findById((long) cartItemID)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));
        return   response;
    }
}
