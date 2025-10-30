package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.BrandDTO;
import com.example.onlyfanshop_be.entity.Brand;
import com.example.onlyfanshop_be.service.BrandService;
import com.example.onlyfanshop_be.service.IBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brands")
public class BrandController {
    @Autowired
    private IBrandService brandService;

    @GetMapping("/")
    public List<Brand> getAllBrandsDetail() {
        return brandService.getAllBrandsDetail();
    }

    @GetMapping("/public")
    public List<BrandDTO> getAllBrands() {
        return brandService.getAllBrands();
    }

    @GetMapping("/{id}")
    public Brand getBrandById(@PathVariable Integer id) {
        return brandService.getBrandById(id);
    }

    @PostMapping("/create")
    public Brand createBrand(@RequestBody BrandDTO brand) {
        return brandService.createBrand(brand);
    }

    @PutMapping("/{id}")
    public Brand updateBrand(@PathVariable int id, @RequestBody BrandDTO brand) {
        return brandService.updateBrand(id, brand);
    }

    @DeleteMapping("/{id}")
    public String deleteBrand(@PathVariable Integer id) {
        brandService.deleteBrand(id);
        return "Xóa thương hiệu có ID " + id + " thành công!";
    }

    @PutMapping("switchActive/{id}")
    public Brand switchActive(@PathVariable int id, @RequestParam boolean active) {
        return brandService.switchActive(id, active);
    }

}
