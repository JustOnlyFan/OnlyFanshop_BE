package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColorRepository extends JpaRepository<Color, Integer> {
    boolean existsByName(String name);
    Optional<Color> findByName(String name);
}

