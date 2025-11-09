package com.example.onlyfanshop_be.repository;



import com.example.onlyfanshop_be.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    java.util.Optional<Category> findByName(String name);
    java.util.Optional<Category> findBySlug(String slug);
}

