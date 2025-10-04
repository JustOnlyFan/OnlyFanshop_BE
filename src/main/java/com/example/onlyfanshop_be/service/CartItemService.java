package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CartItemService implements ICartItemService {
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductRepository productRepository;

    @Override
    public boolean addCartItem(Cart cart, int productID) {
        boolean status = false;
        Product product = productRepository.findByProductID(productID);
        CartItem cartItem = cartItemRepository.findByCart_CartIDAndProduct_ProductID(cart.getCartID(), productID);
        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setPrice(product.getPrice());
            cartItem.setQuantity(1);
            cartItemRepository.save(cartItem);
            status = true;
        }else {
            cartItem.setQuantity(cartItem.getQuantity()+1);
            cartItem.setPrice(product.getPrice()+cartItem.getPrice());
            cartItemRepository.save(cartItem);
            status = true;
        }


        return status;
    }
}
