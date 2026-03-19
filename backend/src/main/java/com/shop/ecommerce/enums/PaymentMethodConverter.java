package com.shop.ecommerce.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        String normalized = dbValue.replace("-", "_").replace(" ", "_");
        for (PaymentMethod m : PaymentMethod.values()) {
            if (m.name().equalsIgnoreCase(normalized)) {
                return m;
            }
        }
        return null;
    }
}
