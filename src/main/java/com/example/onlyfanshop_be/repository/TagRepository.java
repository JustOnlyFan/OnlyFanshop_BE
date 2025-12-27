package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    Optional<Tag> findByCode(String code);

    boolean existsByCode(String code);

    List<Tag> findAllByOrderByDisplayOrderAsc();

    List<Tag> findByCodeIn(List<String> codes);

}
