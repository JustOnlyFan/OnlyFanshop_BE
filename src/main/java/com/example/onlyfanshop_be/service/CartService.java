package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.request.AddToCartRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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
        String username = request.getUserName();
        Integer quantity = request.getQuantity();
        Cart userCart;
        boolean productExist = productRepository.existsById(productID);
        boolean userExist = userRepository.existsByUsername(username);
        if (!productExist) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        } else if (!userExist) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        List<Cart> cartList = cartRepository.findByStatusAndUser_username("InProgress", username);
        if (!cartList.isEmpty()) {
            userCart = cartList.getFirst();
        } else {
            userCart = new Cart();
            userCart.setStatus("InProgress");
            userCart.setTotalPrice(0.0);
            userCart.setUser(userRepository.findByUsername(username).get());
        }
        cartRepository.save(userCart);
        if (cartItemService.addCartItem(userCart, productID, quantity)) {
            userCart.setTotalPrice(userCart.getTotalPrice() + productRepository.findByProductID(productID).getPrice()*quantity);
            cartRepository.save(userCart);
            status = true;
        }

        return status;
    }

    @Override
    public void clearCart(String userName) {
        Cart cart =  cartRepository.findByStatusAndUser_username("InProgress", userName).getFirst();
        cartItemRepository.deleteAll(cart.getCartItems());
        cartRepository.delete(cart);
    }

    public ApiResponse<CartDTO> getCart(int userId) {
        Optional<Cart> cartOptional = cartRepository.findByUser_UserIDAndStatus(userId, "InProgress");
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();

            return ApiResponse.<CartDTO>builder().data(CartDTO.builder().userId(userId).items(cart.getCartItems()).totalQuantity(cart.getCartItems().size()).build()).statusCode(200).message("Lấy giỏ hàng thành công").build();
        }else throw new AppException(ErrorCode.CART_NOTFOUND);
    }
}
