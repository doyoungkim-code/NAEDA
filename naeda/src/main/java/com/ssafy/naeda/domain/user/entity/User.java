package com.ssafy.naeda.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_no")
    private Long userNo;

    @Column(name = "user_id", length = 100, nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String username;

<<<<<<< HEAD
=======
    @Column(name = "resident_no", length = 7, nullable = false)
    private String residentNo;

>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
    @Column(length = 20, nullable = false, unique = true)
    private String phone;

    @Column(name = "institution_code", length = 50, nullable = false)
    private String institutionCode;

    @Column(name = "user_key")
    private String userKey;

    @Column(name = "face_registered", nullable = false)
    @Builder.Default
    private Boolean faceRegistered = false;

<<<<<<< HEAD
=======
    @Column(name = "pin_password")
    private String pinPassword;

>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    private LocalDateTime modified;

    @PreUpdate
    protected void onUpdate() {
        this.modified = LocalDateTime.now();
    }

    public void registerFace() {
        this.faceRegistered = true;
    }

    public void updateUserKey(String userKey) {
        this.userKey = userKey;
    }
}
