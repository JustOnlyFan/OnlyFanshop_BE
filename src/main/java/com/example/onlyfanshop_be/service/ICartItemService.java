package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;

public interface ICartItemService {
    boolean addCartItem(Cart cart, int productID, int quantity, boolean isInstantBuy);
}
