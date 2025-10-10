package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Brand;

import java.util.List;

public interface IBrandService {
    List<Brand> getAllBrands();
    Brand getBrandById(int id);
    Brand updateBrand(int id,Brand brand);
    void deleteBrand(int id);
    Brand createBrand(Brand brand);
}
