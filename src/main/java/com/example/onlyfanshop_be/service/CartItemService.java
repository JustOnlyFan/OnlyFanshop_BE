package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartItemService implements ICartItemService {
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductRepository productRepository;

    @Override
    public boolean addCartItem(Cart cart, int productID,  int quantity, boolean isInstantBuy) {
        boolean status = false;
        Product product = productRepository.findById(productID)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productID));
        
        java.math.BigDecimal unitPrice = product.getBasePrice() != null ? product.getBasePrice() : java.math.BigDecimal.ZERO;
        
        Optional<CartItem> cartItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), (long) productID);
        if (cartItemOpt.isEmpty()) {
            CartItem cartItem = CartItem.builder()
                    .cartId(cart.getId())
                    .productId((long) productID)
                    .quantity(quantity)
                    .unitPriceSnapshot(unitPrice)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            cartItemRepository.save(cartItem);
            status = true;
        } else {
            CartItem cartItem = cartItemOpt.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setUpdatedAt(java.time.LocalDateTime.now());
            cartItemRepository.save(cartItem);
            status = true;
        }

        return status;
    }
}
