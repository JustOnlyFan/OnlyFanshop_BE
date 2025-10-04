package com.example.onlyfanshop_be.repository;


import com.example.onlyfanshop_be.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    List<Cart> findByUser_username(String username);
    List<Cart> findByStatusAndUser_username(String status, String username);
}

