package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    java.util.Optional<Brand> findByName(String name);
    java.util.Optional<Brand> findBySlug(String slug);
}