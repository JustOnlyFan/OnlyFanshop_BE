package com.example.onlyfanshop_be.repository;



import com.example.onlyfanshop_be.entity.Brand;
import com.example.onlyfanshop_be.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByIsActiveTrue();
}

