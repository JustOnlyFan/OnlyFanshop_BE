package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    
    // Legacy methods for backward compatibility
    @Deprecated
    default List<CartItem> findByCart_CartID(Integer CartID) {
        return findByCartId(CartID != null ? CartID.longValue() : null);
    }
    
    @Deprecated
    default CartItem findByCart_CartIDAndProduct_ProductID(Integer CartID, Integer ProductID) {
        return findByCartIdAndProductId(
                CartID != null ? CartID.longValue() : null,
                ProductID != null ? ProductID.longValue() : null
        ).orElse(null);
    }
}

