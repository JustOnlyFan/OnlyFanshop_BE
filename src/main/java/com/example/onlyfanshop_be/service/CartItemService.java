package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
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
        Product product = productRepository.findByProductID(productID);
        CartItem cartItem = cartItemRepository.findByCart_CartIDAndProduct_ProductID(cart.getCartID(), productID);
        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setPrice(product.getPrice()*quantity);
            cartItem.setQuantity(quantity);
            if (isInstantBuy) {
                cartItem.setChecked(true);
            }
            cartItemRepository.save(cartItem);
            status = true;
        }else {
            cartItem.setQuantity(cartItem.getQuantity()+quantity);
            cartItem.setPrice(cartItem.getPrice()+product.getPrice()*quantity);
            cartItemRepository.save(cartItem);
            status = true;
        }


        return status;
    }
}
