package com.ssafy.naeda.domain.face.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * float[] <-> 암호화된 TEXT 변환기
 * DB에는 "base64(IV):base64(암호문)" 형태로 저장
 */
@Component
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    @Autowired
    private EmbeddingEncryptor encryptor;

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(attribute[i]);
        }
        sb.append("]");
        return encryptor.encrypt(sb.toString());
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        String json = encryptor.decrypt(dbData);
        String stripped = json.substring(1, json.length() - 1);
        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
