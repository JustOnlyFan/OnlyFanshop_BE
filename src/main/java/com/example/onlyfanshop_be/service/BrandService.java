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
        List<Brand> brandsToSave = new ArrayList<>(); // Collect brands that need slug fix
        List<BrandDTO> listDTO = new ArrayList<>();
        
        for(Brand brand : list){
            // Đảm bảo brand có slug (fix cho các brand cũ không có slug)
            if (brand.getSlug() == null || brand.getSlug().trim().isEmpty()) {
                String slug = generateSlugForBrand(brand.getName());
                brand.setSlug(slug);
                brandsToSave.add(brand); // Collect để save sau
            }
            
            BrandDTO brandDTO = new BrandDTO();
            brandDTO.setBrandID(brand.getBrandID());
            brandDTO.setName(brand.getBrandName());
            brandDTO.setDescription(brand.getDescription());
            brandDTO.setCountry(null); // Brand entity doesn't have country field
            brandDTO.setImageURL(brand.getImageURL());
            brandDTO.setActive(brand.isActive());
            listDTO.add(brandDTO);
        }
        
        // Save tất cả brands cần fix slug trong một batch
        if (!brandsToSave.isEmpty()) {
            brandRepository.saveAll(brandsToSave);
        }
        
        return listDTO;
    }

    @Override
    public List<Brand> getAllBrandsDetail() {
        List<Brand> brands = brandRepository.findAll();
        List<Brand> brandsToSave = new ArrayList<>(); // Collect brands that need slug fix
        
        // Đảm bảo tất cả brand đều có slug (fix cho các brand cũ không có slug)
        for (Brand brand : brands) {
            if (brand.getSlug() == null || brand.getSlug().trim().isEmpty()) {
                String slug = generateSlugForBrand(brand.getName());
                brand.setSlug(slug);
                brandsToSave.add(brand); // Collect để save sau
            }
        }
        
        // Save tất cả brands cần fix slug trong một batch
        if (!brandsToSave.isEmpty()) {
            brandRepository.saveAll(brandsToSave);
        }
        
        return brands;
    }
    
    @Override
    public Brand getBrandById(int id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));
        
        // Đảm bảo brand có slug (fix cho các brand cũ không có slug)
        if (brand.getSlug() == null || brand.getSlug().trim().isEmpty()) {
            String slug = generateSlugForBrand(brand.getName());
            brand.setSlug(slug);
            brandRepository.save(brand); // Lưu lại để fix vĩnh viễn
        }
        
        return brand;
    }
    @Override
    public Brand createBrand(BrandDTO brand) {
        if (brand.getName() == null || brand.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên thương hiệu không được để trống");
        }

        String brandName = brand.getName().trim();
        if (brandRepository.existsByName(brandName)) {
            throw new RuntimeException("Thương hiệu với tên '" + brandName + "' đã tồn tại");
        }

        String slug = generateSlugForBrand(brandName);

        Brand b = new Brand();
        b.setName(brandName);
        b.setSlug(slug);
        b.setDescription(brand.getDescription());
        // Brand entity doesn't have country field
        b.setLogoUrl(brand.getImageURL());
        // Note: Brand entity doesn't have active field - always active in new schema

        return brandRepository.save(b);
    }

    @Override
    public Brand switchActive(int id, boolean active) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));

        if (brand.getSlug() == null || brand.getSlug().trim().isEmpty()) {
            String slug = generateSlugForBrand(brand.getName());
            brand.setSlug(slug);
        }

        brandRepository.save(brand);
        productService.updateActiveByBrand(brand.getBrandID());
        return brand;
    }

    @Override
    public Brand updateBrand(int id, BrandDTO updatedBrand) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu có ID: " + id));

        if (brand.getSlug() == null || brand.getSlug().trim().isEmpty()) {
            String slug = generateSlugForBrand(brand.getName());
            brand.setSlug(slug);
        }

        if (updatedBrand.getName() != null && !updatedBrand.getName().trim().isEmpty()) {
            String oldName = brand.getName();
            String newName = updatedBrand.getName().trim();

            if (!oldName.equals(newName) && brandRepository.existsByName(newName)) {
                throw new RuntimeException("Thương hiệu với tên '" + newName + "' đã tồn tại");
            }

            if (!oldName.equals(newName)) {
                String newSlug = generateSlugForBrand(newName);
                brand.setSlug(newSlug);
            }
            
            brand.setName(newName);
        }

        if (updatedBrand.getDescription() != null) {
            brand.setDescription(updatedBrand.getDescription());
        }
        
        if (updatedBrand.getImageURL() != null) {
            brand.setLogoUrl(updatedBrand.getImageURL());
        }

        return brandRepository.save(brand);
    }
    @Override
    public void deleteBrand(int id) {
        if (!brandRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy thương hiệu có ID: " + id);
        }
        brandRepository.deleteById(id);
    }
    
    // Helper method to generate slug from brand name
    private String generateSlugForBrand(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            return "brand-" + System.currentTimeMillis();
        }

        String baseSlug = brandName.toLowerCase()
                .trim()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single hyphen
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
        
        if (baseSlug.isEmpty()) {
            baseSlug = "brand";
        }
        
        // Check if slug already exists, if yes, append number
        List<Brand> allBrands = brandRepository.findAll();
        int counter = 0;
        String slug;
        
        do {
            if (counter == 0) {
                slug = baseSlug;
            } else {
                slug = baseSlug + "-" + counter;
            }
            final String currentSlug = slug; // Final variable for lambda
            boolean slugExists = allBrands.stream()
                    .anyMatch(b -> b.getSlug() != null && b.getSlug().equals(currentSlug));
            if (!slugExists) {
                break;
            }
            counter++;
        } while (true);
        
        return slug;
    }
}
