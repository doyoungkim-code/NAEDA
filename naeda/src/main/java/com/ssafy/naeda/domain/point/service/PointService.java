package com.ssafy.naeda.domain.point.service;

import com.ssafy.naeda.domain.point.dto.request.PointEarnRequest;
import com.ssafy.naeda.domain.point.dto.request.PointUseRequest;
import com.ssafy.naeda.domain.point.dto.response.PointHistoryResponse;
import com.ssafy.naeda.domain.point.dto.response.PointWalletResponse;
import com.ssafy.naeda.domain.point.entity.PointHistory;
import com.ssafy.naeda.domain.point.entity.PointType;
import com.ssafy.naeda.domain.point.entity.PointWallet;
import com.ssafy.naeda.domain.point.repository.PointHistoryRepository;
import com.ssafy.naeda.domain.point.repository.PointWalletRepository;
import com.ssafy.naeda.global.exception.DuplicateException;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public PointWalletResponse createWallet(Long userNo) {
        pointWalletRepository.findByUserNo(userNo).ifPresent(w -> {
            throw new DuplicateException("이미 포인트 지갑이 존재합니다. userNo: " + userNo);
        });

        PointWallet wallet = pointWalletRepository.save(
                PointWallet.builder()
                        .userNo(userNo)
                        .build()
        );

        return PointWalletResponse.from(wallet);
    }

    public PointWalletResponse getWallet(Long userNo) {
        PointWallet wallet = pointWalletRepository.findByUserNo(userNo)
                .orElseThrow(() -> new NotFoundException("포인트 지갑이 존재하지 않습니다. userNo: " + userNo));

        return PointWalletResponse.from(wallet);
    }

    @Transactional
    public PointWalletResponse earnPoints(Long userNo, PointEarnRequest request) {
        PointWallet wallet = pointWalletRepository.findByUserNoForUpdate(userNo)
                .orElseThrow(() -> new NotFoundException("포인트 지갑이 존재하지 않습니다. userNo: " + userNo));

        wallet.earn(request.getAmount());

        pointHistoryRepository.save(
                PointHistory.builder()
                        .walletId(wallet.getWalletId())
                        .type(PointType.EARN)
                        .amount(request.getAmount())
                        .balanceAfter(wallet.getBalance())
                        .description(request.getDescription())
                        .paymentId(request.getPaymentId())
                        .build()
        );

        return PointWalletResponse.from(wallet);
    }

    @Transactional
    public PointWalletResponse usePoints(Long userNo, PointUseRequest request) {
        PointWallet wallet = pointWalletRepository.findByUserNoForUpdate(userNo)
                .orElseThrow(() -> new NotFoundException("포인트 지갑이 존재하지 않습니다. userNo: " + userNo));

        wallet.use(request.getAmount());

        pointHistoryRepository.save(
                PointHistory.builder()
                        .walletId(wallet.getWalletId())
                        .type(PointType.USE_COUPON)
                        .amount(request.getAmount())
                        .balanceAfter(wallet.getBalance())
                        .description(request.getDescription())
                        .build()
        );

        return PointWalletResponse.from(wallet);
    }

    public List<PointHistoryResponse> getHistories(Long userNo) {
        PointWallet wallet = pointWalletRepository.findByUserNo(userNo)
                .orElseThrow(() -> new NotFoundException("포인트 지갑이 존재하지 않습니다. userNo: " + userNo));

        return pointHistoryRepository.findByWalletIdOrderByCreatedDesc(wallet.getWalletId())
                .stream()
                .map(PointHistoryResponse::from)
                .toList();
    }
}
