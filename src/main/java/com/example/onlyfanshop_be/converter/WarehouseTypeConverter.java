package com.example.onlyfanshop_be.converter;

import com.example.onlyfanshop_be.enums.WarehouseType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class WarehouseTypeConverter implements AttributeConverter<WarehouseType, String> {

    @Override
    public String convertToDatabaseColumn(WarehouseType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }

    @Override
    public WarehouseType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return WarehouseType.fromDbValue(dbData);
    }
}

