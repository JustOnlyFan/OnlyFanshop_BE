package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Warranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Integer> {
    boolean existsByName(String name);
    Optional<Warranty> findByName(String name);
}






