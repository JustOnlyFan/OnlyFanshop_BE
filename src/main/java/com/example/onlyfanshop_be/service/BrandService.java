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
    @Override
    public List<BrandDTO> getAllBrands() {
        List<Brand>list = brandRepository.findAll();
        List<BrandDTO> listDTO = new ArrayList<>();
        for(Brand brand : list){
            BrandDTO brandDTO = new BrandDTO();
            brandDTO.setBrandID(brand.getBrandID());
            brandDTO.setName(brand.getBrandName());
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
