package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.SalesChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesChannelRepository extends JpaRepository<SalesChannel, Integer> {
    Optional<SalesChannel> findByCode(String code);
}







