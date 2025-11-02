package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.request.AddToCartRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;

public interface ICartService {
     boolean addToCart(AddToCartRequest request);
     void clearCart(String userName);
     Cart instantBuy(AddToCartRequest request);
     void deleteInstantCart(Integer userID);
}
