package com.ssafy.naeda.domain.user.repository;

import com.ssafy.naeda.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserNo(Long userNo);
}
