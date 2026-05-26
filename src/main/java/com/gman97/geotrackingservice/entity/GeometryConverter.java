package com.gman97.geotrackingservice.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GeometryConverter implements AttributeConverter<Geometry, String> {

    @Override
    public String convertToDatabaseColumn(Geometry field) {
        return field != null
                ? field.name().toLowerCase()
                : null;
    }

    @Override
    public Geometry convertToEntityAttribute(String dbString) {
        return dbString != null
                ? Geometry.valueOf(dbString.toUpperCase())
                : null;
    }
}
