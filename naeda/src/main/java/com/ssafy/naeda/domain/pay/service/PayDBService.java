package com.ssafy.naeda.domain.pay.service;

import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import com.ssafy.naeda.domain.pay.repository.PayTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayDBService {

    private final PayTransactionRepository payTransactionRepository;

    @Transactional
    public PayTransaction save(PayTransaction transaction) {
        return payTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Optional<PayTransaction> findById(Long id) {
        return payTransactionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PayTransaction> findByUserNo(Long userNo) {
        return payTransactionRepository.findByUserNoOrderByCreatedAtDesc(userNo);
    }

    @Transactional(readOnly = true)
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return payTransactionRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Transactional(readOnly = true)
    public Optional<PayTransaction> findByIdempotencyKey(String idempotencyKey) {
        return payTransactionRepository.findByIdempotencyKey(idempotencyKey);
    }
}
