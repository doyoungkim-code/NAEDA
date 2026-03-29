package com.ssafy.naeda.domain.identity.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ResidentIdVerificationSessionService {

    private static final long SESSION_TTL_SECONDS = 20 * 60L;

    private final Map<String, VerificationSession> sessions = new ConcurrentHashMap<>();

    public void recordExtraction(String userId, boolean hasRecognizedIdentity) {
        if (!hasRecognizedIdentity) {
            sessions.remove(userId);
            return;
        }
        sessions.put(userId, VerificationSession.start());
    }

    public boolean hasActiveExtraction(String userId) {
        cleanupExpired(userId);
        VerificationSession session = sessions.get(userId);
        return session != null && session.hasRecognizedIdentity();
    }

    public boolean isConfirmed(String userId) {
        cleanupExpired(userId);
        VerificationSession session = sessions.get(userId);
        return session != null && session.isConfirmed();
    }

    public void markConfirmed(String userId) {
        VerificationSession session = getActiveSession(userId);
        session.confirm();
    }

    public void resetConfirmation(String userId) {
        VerificationSession session = sessions.get(userId);
        if (session == null) {
            return;
        }
        if (session.isExpired()) {
            sessions.remove(userId);
            return;
        }
        session.resetConfirmation();
    }

    public void clearSession(String userId) {
        sessions.remove(userId);
    }

    private VerificationSession getActiveSession(String userId) {
        cleanupExpired(userId);
        VerificationSession session = sessions.get(userId);
        if (session == null) {
            throw new IllegalStateException("신분증 OCR 세션이 만료되었습니다.");
        }
        return session;
    }

    private void cleanupExpired(String userId) {
        VerificationSession session = sessions.get(userId);
        if (session != null && session.isExpired()) {
            sessions.remove(userId);
        }
    }

    private static class VerificationSession {
        private final long expiresAtEpochSeconds;
        private final boolean recognizedIdentity;
        private volatile boolean confirmed;

        private VerificationSession(long expiresAtEpochSeconds, boolean recognizedIdentity) {
            this.expiresAtEpochSeconds = expiresAtEpochSeconds;
            this.recognizedIdentity = recognizedIdentity;
        }

        static VerificationSession start() {
            return new VerificationSession(
                    (System.currentTimeMillis() / 1000L) + SESSION_TTL_SECONDS,
                    true
            );
        }

        boolean hasRecognizedIdentity() {
            return recognizedIdentity;
        }

        boolean isConfirmed() {
            return confirmed;
        }

        void confirm() {
            this.confirmed = true;
        }

        void resetConfirmation() {
            this.confirmed = false;
        }

        boolean isExpired() {
            return (System.currentTimeMillis() / 1000L) > expiresAtEpochSeconds;
        }
    }
}
