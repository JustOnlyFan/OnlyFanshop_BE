package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Brand;
import com.example.onlyfanshop_be.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandService implements IBrandService{
    @Autowired
    private BrandRepository brandRepository;
    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }
    @Override
    public Brand getBrandById(int id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));
    }
    @Override
    public Brand createBrand(Brand brand) {
        return brandRepository.save(brand);
    }
    @Override
    public Brand updateBrand(int id, Brand updatedBrand) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));

        brand.setBrandName(updatedBrand.getBrandName());
        brand.setCountry(updatedBrand.getCountry());
        brand.setDescription(updatedBrand.getDescription());

        return brandRepository.save(brand);
    }
    @Override
    public void deleteBrand(int id) {
        if (!brandRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy thương hiệu có ID: " + id);
        }
        brandRepository.deleteById(id);
    }
}
