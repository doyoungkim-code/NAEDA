package com.ssafy.naeda.domain.face.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * float[] <-> TEXT (JSON 배열 형태) 변환기
 * DB에는 ENCv1 포맷(AES-GCM) 또는 기존 JSON 문자열로 저장
 */
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    private static final String PREFIX = "ENCv1";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        String plainJson = toJson(attribute);
        SecretKeySpec keySpec = resolveKeySpec();
        if (keySpec == null) {
            // 로컬 개발 환경 호환을 위해 키가 없으면 평문(JSON)으로 저장
            return plainJson;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainJson.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ":" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt face embedding", e);
        }
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        if (dbData.startsWith(PREFIX + ":")) {
            SecretKeySpec keySpec = resolveKeySpec();
            if (keySpec == null) {
                throw new IllegalStateException("FACE_EMBEDDING_AES_KEY is required to decrypt face embedding");
            }
            String plainJson = decrypt(dbData, keySpec);
            return parseJsonArray(plainJson);
        }
        return parseJsonArray(dbData);
    }

    private String toJson(float[] attribute) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(attribute[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] parseJsonArray(String dbData) {
        String stripped = dbData.substring(1, dbData.length() - 1);
        if (stripped.isBlank()) {
            return new float[0];
        }
        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private String decrypt(String cipherText, SecretKeySpec keySpec) {
        try {
            String[] tokens = cipherText.split(":");
            if (tokens.length != 3) {
                throw new IllegalStateException("Invalid encrypted embedding format");
            }
            byte[] iv = Base64.getDecoder().decode(tokens[1]);
            byte[] encrypted = Base64.getDecoder().decode(tokens[2]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt face embedding", e);
        }
    }

    private SecretKeySpec resolveKeySpec() {
        String key = System.getenv("FACE_EMBEDDING_AES_KEY");
        if (key == null || key.isBlank()) {
            return null;
        }
        byte[] keyBytes = key.trim().getBytes(StandardCharsets.UTF_8);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalStateException("FACE_EMBEDDING_AES_KEY length must be 16, 24, or 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
