package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.request.AddToCartRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService implements ICartService {
    @Autowired
    CartRepository cartRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CartItemService cartItemService;
    @Autowired
    private CartItemRepository cartItemRepository;

    @Override
    public boolean addToCart(AddToCartRequest request) throws AppException {
        boolean status = false;
        Integer productID = request.getProductId();
        String userName = request.getUserName(); // Can be email or userId
        Integer quantity = request.getQuantity();
        Cart userCart;
        boolean productExist = productRepository.existsById(productID);
        
        // Try to find user by email first, if not found, try as userId
        User user = null;
        if (userName != null) {
            Optional<User> userOpt = userRepository.findByEmail(userName);
            if (userOpt.isEmpty() && userName.matches("\\d+")) {
                // Try as userId if it's a number
                userOpt = userRepository.findById(Long.parseLong(userName));
            }
            user = userOpt.orElse(null);
        }
        
        if (!productExist) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        } else if (user == null) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        
        // Find existing cart for user (no status field in new schema)
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isPresent()) {
            userCart = cartOpt.get();
        } else {
            userCart = Cart.builder()
                    .userId(user.getId())
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
        }
        cartRepository.save(userCart);
        if (cartItemService.addCartItem(userCart, productID, quantity, false)) {
            userCart.setUpdatedAt(java.time.LocalDateTime.now());
            cartRepository.save(userCart);
            status = true;
        }

        return status;
    }

    @Override
    public void clearCart(String userName) {
        // Try to find user by email first, if not found, try as userId
        User user = null;
        if (userName != null) {
            Optional<User> userOpt = userRepository.findByEmail(userName);
            if (userOpt.isEmpty() && userName.matches("\\d+")) {
                // Try as userId if it's a number
                userOpt = userRepository.findById(Long.parseLong(userName));
            }
            user = userOpt.orElse(null);
        }
        
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cartItemRepository.deleteAll(cart.getCartItems());
            cartRepository.delete(cart);
        }
    }

    @Override
    public Cart instantBuy(AddToCartRequest request) {
        Integer productID = request.getProductId();
        String userName = request.getUserName(); // Can be email or userId
        Integer quantity = request.getQuantity();

        // Try to find user by email first, if not found, try as userId
        User user = null;
        if (userName != null) {
            Optional<User> userOpt = userRepository.findByEmail(userName);
            if (userOpt.isEmpty() && userName.matches("\\d+")) {
                // Try as userId if it's a number
                userOpt = userRepository.findById(Long.parseLong(userName));
            }
            user = userOpt.orElse(null);
        }

        boolean productExist = productRepository.existsById(productID);
        if (!productExist) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        } else if (user == null) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        
        // Clear existing cart for instant buy (no status field in new schema)
        Optional<Cart> existingCartOpt = cartRepository.findByUserId(user.getId());
        if (existingCartOpt.isPresent()) {
            Cart existingCart = existingCartOpt.get();
            cartItemRepository.deleteAll(existingCart.getCartItems());
            cartRepository.delete(existingCart);
        }
        
        // Create new cart for instant buy
        Cart cart = Cart.builder()
                .userId(user.getId())
                .createdAt(java.time.LocalDateTime.now())
                .build();
        cartRepository.save(cart);
        if (cartItemService.addCartItem(cart, productID, quantity, true)) {
            cart.setUpdatedAt(java.time.LocalDateTime.now());
            cartRepository.save(cart);
        }
        return cart;
    }

    @Override
    public void deleteInstantCart(Integer userID) {
        // Note: Status field removed, just delete user's cart
        Optional<Cart> cartOpt = cartRepository.findByUserId((long) userID);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cartItemRepository.deleteAll(cart.getCartItems());
            cartRepository.delete(cart);
        } else {
            throw new AppException(ErrorCode.CART_NOTFOUND);
        }
    }

    public ApiResponse<CartDTO> getCart(int userId, String status) {
        // Note: Status field removed, just get user's cart
        Optional<Cart> cartOptional = cartRepository.findByUserId((long) userId);
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            return ApiResponse.<CartDTO>builder()
                    .data(CartDTO.builder()
                            .userId(userId)
                            .items(cart.getCartItems() != null ? cart.getCartItems() : java.util.Collections.emptyList())
                            .totalQuantity(cart.getCartItems() != null ? cart.getCartItems().size() : 0)
                            .build())
                    .statusCode(200)
                    .message("Lấy giỏ hàng thành công")
                    .build();
        } else {
            throw new AppException(ErrorCode.CART_NOTFOUND);
        }
    }
}
