package com.shop.ecommerce.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ShipmentStatusConverter implements AttributeConverter<ShipmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(ShipmentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ShipmentStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        String normalized = dbValue.replace("-", "_").replace(" ", "_");
        for (ShipmentStatus s : ShipmentStatus.values()) {
            if (s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
        }
        return null;
    }
}
