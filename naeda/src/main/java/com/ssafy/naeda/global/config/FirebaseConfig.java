package com.ssafy.naeda.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("[Firebase] fcm.credentials-path 미설정 — FCM 발송이 비활성화됩니다.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("[Firebase] 이미 초기화되어 있습니다.");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("[Firebase] 초기화 완료.");
        } catch (IOException e) {
            log.error("[Firebase] 초기화 실패 — credentials 파일을 읽을 수 없습니다: {}", credentialsPath, e);
        }
    }
}
