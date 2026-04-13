// PaymentProvider enum'u için JPA AttributeConverter
// Veritabanındaki String değeri ile Java enum arasında dönüşüm yapar
package com.shop.ecommerce.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentProviderConverter implements AttributeConverter<PaymentProvider, String> {

    @Override
    public String convertToDatabaseColumn(PaymentProvider attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PaymentProvider convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        String normalized = dbValue.replace("-", "_").replace(" ", "_");
        for (PaymentProvider p : PaymentProvider.values()) {
            if (p.name().equalsIgnoreCase(normalized)) {
                return p;
            }
        }
        return null;
    }
}
