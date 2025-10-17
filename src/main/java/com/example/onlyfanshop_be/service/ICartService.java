package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;

public interface ICartService {
     boolean addToCart(int productID, String userName);
     void clearCart(String userName);
}
