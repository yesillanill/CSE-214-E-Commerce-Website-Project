package com.shop.ecommerce.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ShippingMethodConverter implements AttributeConverter<ShippingMethod, String> {

    @Override
    public String convertToDatabaseColumn(ShippingMethod attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ShippingMethod convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        String normalized = dbValue.replace("-", "_").replace(" ", "_");
        for (ShippingMethod m : ShippingMethod.values()) {
            if (m.name().equalsIgnoreCase(normalized)) {
                return m;
            }
        }
        return null;
    }
}
