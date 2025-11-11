package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.entity.Color;
import com.example.onlyfanshop_be.service.ColorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/colors")
public class ColorController {
    @Autowired
    private ColorService colorService;

    @GetMapping("/public")
    public List<Color> getAllColors() {
        return colorService.getAllColors();
    }

    @GetMapping("/{id}")
    public Color getColorById(@PathVariable Integer id) {
        return colorService.getColorById(id);
    }

    @PostMapping("/create")
    public Color createColor(@RequestBody Color color) {
        return colorService.createColor(color);
    }

    @PutMapping("/{id}")
    public Color updateColor(@PathVariable Integer id, @RequestBody Color color) {
        return colorService.updateColor(id, color);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColor(@PathVariable Integer id) {
        colorService.deleteColor(id);
        return ResponseEntity.noContent().build();
    }
}






