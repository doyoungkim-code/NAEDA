package com.ssafy.naeda.domain.face.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

<<<<<<< HEAD
/**
 * float[] <-> 암호화된 TEXT 변환기
 * DB에는 "base64(IV):base64(암호문)" 형태로 저장
=======
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * float[] <-> 암호화된 TEXT 변환기
 * 저장 형식:
 * 1) 현재: base64(IV):base64(암호문)
 * 2) 호환: ENCv1:base64(IV):base64(암호문), JSON 평문 배열
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
 */
@Component
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

<<<<<<< HEAD
=======
    private static final String LEGACY_PREFIX = "ENCv1:";
    private static final int GCM_TAG_BITS = 128;

>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
    @Autowired
    private EmbeddingEncryptor encryptor;

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
<<<<<<< HEAD
=======
        return encryptor.encrypt(toJson(attribute));
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;

        String json;
        if (dbData.startsWith("[")) {
            // 평문 JSON 레거시 데이터 호환
            json = dbData;
        } else if (dbData.startsWith(LEGACY_PREFIX)) {
            // ENCv1 포맷 호환
            json = decryptLegacy(dbData);
        } else {
            // 현재 암호화 포맷(iv:ciphertext)
            json = encryptor.decrypt(dbData);
        }
        return parseJsonArray(json);
    }

    private String toJson(float[] attribute) {
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(attribute[i]);
        }
        sb.append("]");
<<<<<<< HEAD
        return encryptor.encrypt(sb.toString());
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        String json = encryptor.decrypt(dbData);
        String stripped = json.substring(1, json.length() - 1);
=======
        return sb.toString();
    }

    private float[] parseJsonArray(String json) {
        String stripped = json.substring(1, json.length() - 1);
        if (stripped.isBlank()) {
            return new float[0];
        }
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
<<<<<<< HEAD
=======

    private String decryptLegacy(String cipherText) {
        String key = System.getenv("FACE_EMBEDDING_AES_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("FACE_EMBEDDING_AES_KEY is required to decrypt legacy ENCv1 embedding");
        }
        byte[] keyBytes = key.trim().getBytes(StandardCharsets.UTF_8);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalStateException("FACE_EMBEDDING_AES_KEY length must be 16, 24, or 32 bytes");
        }

        try {
            String[] tokens = cipherText.split(":");
            if (tokens.length != 3) {
                throw new IllegalStateException("Invalid ENCv1 embedding format");
            }
            byte[] iv = Base64.getDecoder().decode(tokens[1]);
            byte[] encrypted = Base64.getDecoder().decode(tokens[2]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt legacy ENCv1 embedding", e);
        }
    }
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
}
