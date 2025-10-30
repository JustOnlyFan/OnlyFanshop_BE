package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.BrandDTO;
import com.example.onlyfanshop_be.entity.Brand;
import com.example.onlyfanshop_be.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BrandService implements IBrandService{
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private ProductService productService;
    @Override
    public List<BrandDTO> getAllBrands() {
        List<Brand>list = brandRepository.findAll();
        List<BrandDTO> listDTO = new ArrayList<>();
        for(Brand brand : list){
            BrandDTO brandDTO = new BrandDTO();
            brandDTO.setBrandID(brand.getBrandID());
            brandDTO.setName(brand.getBrandName());
            brandDTO.setDescription(brand.getDescription());
            brandDTO.setCountry(brand.getCountry());
            brandDTO.setActive(brand.isActive());
            listDTO.add(brandDTO);
        }
        return listDTO;
    }

    @Override
    public List<Brand> getAllBrandsDetail() {
        return brandRepository.findAll();
    }
    @Override
    public Brand getBrandById(int id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));
    }
    @Override
    public Brand createBrand(BrandDTO brand) {
        Brand b = new Brand();
        b.setBrandName(brand.getName());
        b.setDescription(brand.getDescription());
        b.setCountry(brand.getCountry());
        return brandRepository.save(b);
    }

    @Override
    public Brand switchActive(int id, boolean active) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));

        brand.setActive(active);
        brandRepository.save(brand);
        productService.updateActiveByBrand(brand.getBrandID());
        return brand;
    }

    @Override
    public Brand updateBrand(int id, BrandDTO updatedBrand) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));

        brand.setBrandName(updatedBrand.getName());
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
