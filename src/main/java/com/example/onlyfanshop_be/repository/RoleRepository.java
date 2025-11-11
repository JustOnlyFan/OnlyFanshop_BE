package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Byte> {
    Optional<Role> findByName(String name);
}







