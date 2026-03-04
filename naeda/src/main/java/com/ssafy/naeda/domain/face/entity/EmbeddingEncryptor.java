package com.ssafy.naeda.domain.face.entity;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EmbeddingEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    @Value("${face.encrypt-key}")
    private String base64Key;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes());

            String ivB64 = Base64.getEncoder().encodeToString(iv);
            String ctB64 = Base64.getEncoder().encodeToString(cipherText);
            return ivB64 + ":" + ctB64;
        } catch (Exception e) {
            throw new RuntimeException("임베딩 암호화 실패", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            String[] parts = encryptedData.split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes);
        } catch (Exception e) {
            throw new RuntimeException("임베딩 복호화 실패", e);
        }
    }
}
