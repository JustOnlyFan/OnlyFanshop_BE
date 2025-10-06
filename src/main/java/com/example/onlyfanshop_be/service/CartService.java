package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService implements  ICartService {
    @Autowired
    CartRepository cartRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CartItemService cartItemService;

    @Override
    public boolean addToCart(int productID, String username) {
        boolean status = false;
        Cart userCart;
        boolean  productExist = productRepository.existsById(productID);
        boolean userExist  = userRepository.existsByUsername(username);
        if(!productExist){
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }else if(!userExist){
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        List<Cart> cartList = cartRepository.findByStatusAndUser_username("InProgress", username);
        if(!cartList.isEmpty()){
           userCart = cartList.getFirst();
        }else{
            userCart = new Cart();
            userCart.setStatus("InProgress");
            userCart.setTotalPrice(0.0);
            userCart.setUser(userRepository.findByUsername(username).get());
        }
        cartRepository.save(userCart);
        if (cartItemService.addCartItem(userCart, productID)) {
            userCart.setTotalPrice(userCart.getTotalPrice()+productRepository.findByProductID(productID).getPrice());
            cartRepository.save(userCart);
            status = true;
        }

        return status;
    }
}
