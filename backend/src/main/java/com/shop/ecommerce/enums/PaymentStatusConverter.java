package com.shop.ecommerce.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        String normalized = dbValue.replace("-", "_").replace(" ", "_");
        for (PaymentStatus s : PaymentStatus.values()) {
            if (s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
        }
        return null;
    }
}
