package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.BrandDTO;
import com.example.onlyfanshop_be.dto.CategoryDTO;
import com.example.onlyfanshop_be.repository.BrandRepository;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class CacheService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private BrandRepository brandRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    // Cache with TTL (Time To Live) - 30 minutes for categories/brands, 10 minutes for prices
    private static final long CACHE_TTL_MS = 30 * 60 * 1000;
    private static final long PRICE_CACHE_TTL_MS = 10 * 60 * 1000;
    
    private List<CategoryDTO> cachedCategories = null;
    private List<BrandDTO> cachedBrands = null;
    private AtomicLong categoriesCacheTime = new AtomicLong(0);
    private AtomicLong brandsCacheTime = new AtomicLong(0);
    
    // Cache for min/max price (they don't change often)
    private Long cachedMinPrice = null;
    private Long cachedMaxPrice = null;
    private AtomicLong priceCacheTime = new AtomicLong(0);

    public List<CategoryDTO> getCategories() {
        long now = System.currentTimeMillis();
        if (cachedCategories == null || (now - categoriesCacheTime.get()) > CACHE_TTL_MS) {
            synchronized (this) {
                // Double-check locking
                if (cachedCategories == null || (now - categoriesCacheTime.get()) > CACHE_TTL_MS) {
                    cachedCategories = categoryRepository.findAll().stream()
                            .map(c -> CategoryDTO.simple(c.getCategoryID(), c.getCategoryName()))
                            .collect(Collectors.toList());
                    categoriesCacheTime.set(now);
                }
            }
        }
        return cachedCategories;
    }

    public List<BrandDTO> getBrands() {
        long now = System.currentTimeMillis();
        if (cachedBrands == null || (now - brandsCacheTime.get()) > CACHE_TTL_MS) {
            synchronized (this) {
                // Double-check locking
                if (cachedBrands == null || (now - brandsCacheTime.get()) > CACHE_TTL_MS) {
                    cachedBrands = brandRepository.findAll().stream()
                            .map(b -> BrandDTO.builder()
                                    .brandID(b.getBrandID() == null ? null : b.getBrandID().intValue())
                                    .name(b.getBrandName())
                                    .imageURL(b.getImageURL())
                                    .build())
                            .collect(Collectors.toList());
                    brandsCacheTime.set(now);
                }
            }
        }
        return cachedBrands;
    }

    public void invalidateCategoriesCache() {
        synchronized (this) {
            cachedCategories = null;
            categoriesCacheTime.set(0);
        }
    }
    
    public void invalidateBrandsCache() {
        synchronized (this) {
            cachedBrands = null;
            brandsCacheTime.set(0);
        }
    }

    public java.util.Map<String, Long> getPriceRange() {
        try {
            long now = System.currentTimeMillis();
            if (cachedMinPrice == null || cachedMaxPrice == null || (now - priceCacheTime.get()) > PRICE_CACHE_TTL_MS) {
                synchronized (this) {
                    // Double-check locking
                    if (cachedMinPrice == null || cachedMaxPrice == null || (now - priceCacheTime.get()) > PRICE_CACHE_TTL_MS) {
                        BigDecimal maxPriceBD = productRepository.findMaxPrice();
                        BigDecimal minPriceBD = productRepository.findMinPrice();
                        cachedMaxPrice = maxPriceBD != null ? maxPriceBD.longValue() : null;
                        cachedMinPrice = minPriceBD != null ? minPriceBD.longValue() : null;
                        priceCacheTime.set(now);
                    }
                }
            }
            // Use HashMap instead of Map.of() for better compatibility
            java.util.Map<String, Long> result = new java.util.HashMap<>();
            result.put("minPrice", cachedMinPrice != null ? cachedMinPrice : 0L);
            result.put("maxPrice", cachedMaxPrice != null ? cachedMaxPrice : 0L);
            return result;
        } catch (Exception e) {
            System.err.println("Error in CacheService.getPriceRange(): " + e.getMessage());
            e.printStackTrace();
            // Return default values on error
            java.util.Map<String, Long> result = new java.util.HashMap<>();
            result.put("minPrice", 0L);
            result.put("maxPrice", 0L);
            return result;
        }
    }
    
    public void invalidatePriceCache() {
        synchronized (this) {
            cachedMinPrice = null;
            cachedMaxPrice = null;
            priceCacheTime.set(0);
        }
    }
    
    public void invalidateAllCache() {
        invalidateCategoriesCache();
        invalidateBrandsCache();
        invalidatePriceCache();
    }
}

