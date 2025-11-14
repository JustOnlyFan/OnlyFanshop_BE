package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Color;
import com.example.onlyfanshop_be.repository.ColorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ColorService {
    @Autowired
    private ColorRepository colorRepository;

    public List<Color> getAllColors() {
        return colorRepository.findAll();
    }

    public Color getColorById(Integer id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc có ID: " + id));
    }

    public Color createColor(Color color) {
        // Validate name
        if (color.getName() == null || color.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên màu sắc không được để trống");
        }

        // Check if name already exists
        String colorName = color.getName().trim();
        if (colorRepository.existsByName(colorName)) {
            throw new RuntimeException("Màu sắc với tên '" + colorName + "' đã tồn tại");
        }

        Color c = new Color();
        c.setName(colorName);
        c.setHexCode(color.getHexCode());
        c.setDescription(color.getDescription());

        return colorRepository.save(c);
    }

    public Color updateColor(Integer id, Color updatedColor) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc có ID: " + id));

        if (updatedColor.getName() != null && !updatedColor.getName().trim().isEmpty()) {
            String newName = updatedColor.getName().trim();
            if (!color.getName().equals(newName) && colorRepository.existsByName(newName)) {
                throw new RuntimeException("Màu sắc với tên '" + newName + "' đã tồn tại");
            }
            color.setName(newName);
        }

        if (updatedColor.getHexCode() != null) {
            color.setHexCode(updatedColor.getHexCode());
        }

        if (updatedColor.getDescription() != null) {
            color.setDescription(updatedColor.getDescription());
        }

        return colorRepository.save(color);
    }

    public void deleteColor(Integer id) {
        if (!colorRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy màu sắc có ID: " + id);
        }
        colorRepository.deleteById(id);
    }
}











