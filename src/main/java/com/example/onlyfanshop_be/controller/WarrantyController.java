package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.entity.Warranty;
import com.example.onlyfanshop_be.service.WarrantyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warranties")
public class WarrantyController {
    @Autowired
    private WarrantyService warrantyService;

    @GetMapping("/public")
    public List<Warranty> getAllWarranties() {
        return warrantyService.getAllWarranties();
    }

    @GetMapping("/{id}")
    public Warranty getWarrantyById(@PathVariable Integer id) {
        return warrantyService.getWarrantyById(id);
    }

    @PostMapping("/create")
    public Warranty createWarranty(@RequestBody Warranty warranty) {
        return warrantyService.createWarranty(warranty);
    }

    @PutMapping("/{id}")
    public Warranty updateWarranty(@PathVariable Integer id, @RequestBody Warranty warranty) {
        return warrantyService.updateWarranty(id, warranty);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarranty(@PathVariable Integer id) {
        warrantyService.deleteWarranty(id);
        return ResponseEntity.noContent().build();
    }
}












