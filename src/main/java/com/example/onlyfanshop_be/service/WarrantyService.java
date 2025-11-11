package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Warranty;
import com.example.onlyfanshop_be.repository.WarrantyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarrantyService {
    @Autowired
    private WarrantyRepository warrantyRepository;

    public List<Warranty> getAllWarranties() {
        return warrantyRepository.findAll();
    }

    public Warranty getWarrantyById(Integer id) {
        return warrantyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bảo hành có ID: " + id));
    }

    public Warranty createWarranty(Warranty warranty) {
        // Validate name
        if (warranty.getName() == null || warranty.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên bảo hành không được để trống");
        }

        // Validate duration
        if (warranty.getDurationMonths() == null || warranty.getDurationMonths() <= 0) {
            throw new RuntimeException("Thời gian bảo hành phải lớn hơn 0");
        }

        // Check if name already exists
        String warrantyName = warranty.getName().trim();
        if (warrantyRepository.existsByName(warrantyName)) {
            throw new RuntimeException("Bảo hành với tên '" + warrantyName + "' đã tồn tại");
        }

        Warranty w = new Warranty();
        w.setName(warrantyName);
        w.setDurationMonths(warranty.getDurationMonths());
        w.setDescription(warranty.getDescription());
        w.setTermsAndConditions(warranty.getTermsAndConditions());
        w.setCoverage(warranty.getCoverage());

        return warrantyRepository.save(w);
    }

    public Warranty updateWarranty(Integer id, Warranty updatedWarranty) {
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bảo hành có ID: " + id));

        if (updatedWarranty.getName() != null && !updatedWarranty.getName().trim().isEmpty()) {
            String newName = updatedWarranty.getName().trim();
            if (!warranty.getName().equals(newName) && warrantyRepository.existsByName(newName)) {
                throw new RuntimeException("Bảo hành với tên '" + newName + "' đã tồn tại");
            }
            warranty.setName(newName);
        }

        if (updatedWarranty.getDurationMonths() != null && updatedWarranty.getDurationMonths() > 0) {
            warranty.setDurationMonths(updatedWarranty.getDurationMonths());
        }

        if (updatedWarranty.getDescription() != null) {
            warranty.setDescription(updatedWarranty.getDescription());
        }

        if (updatedWarranty.getTermsAndConditions() != null) {
            warranty.setTermsAndConditions(updatedWarranty.getTermsAndConditions());
        }

        if (updatedWarranty.getCoverage() != null) {
            warranty.setCoverage(updatedWarranty.getCoverage());
        }

        return warrantyRepository.save(warranty);
    }

    public void deleteWarranty(Integer id) {
        if (!warrantyRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy thông tin bảo hành có ID: " + id);
        }
        warrantyRepository.deleteById(id);
    }
}






