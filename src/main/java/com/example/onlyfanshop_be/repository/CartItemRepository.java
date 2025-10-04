package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart_CartID(Integer CartID);
    CartItem findByCart_CartIDAndProduct_ProductID(Integer CartID, Integer ProductID);
}

