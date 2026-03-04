package com.ssafy.naeda.domain.face.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * float[] <-> TEXT (JSON 배열 형태) 변환기
 * DB에는 "[0.12,-0.03,0.09,...]" 문자열로 저장
 */
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(attribute[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        String stripped = dbData.substring(1, dbData.length() - 1);
        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
