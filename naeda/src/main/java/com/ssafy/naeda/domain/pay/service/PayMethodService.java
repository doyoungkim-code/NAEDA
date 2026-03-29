package com.ssafy.naeda.domain.pay.service;

import com.ssafy.naeda.domain.pay.entity.PayMethod;
import com.ssafy.naeda.domain.pay.repository.PayMethodRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayMethodService {

    private final PayMethodRepository payMethodRepository;

    @Transactional(readOnly = true)
    public List<PayMethod> getActiveMethods(Long userNo) {
        return payMethodRepository.findByUserNoAndIsActiveTrue(userNo);
    }

    /**
     * 대표 결제수단 설정 = 페이스페이 결제수단 설정 (통합)
     * 기존 대표/페이스페이 설정을 모두 해제하고, 대상에 둘 다 설정한다.
     */
    @Transactional
    public PayMethod setDefault(Long userNo, Long paymentMethodId) {
        List<PayMethod> methods = payMethodRepository.findByUserNoAndIsActiveTrue(userNo);

        PayMethod target = methods.stream()
                .filter(method -> method.getPaymentMethodId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("결제 수단을 찾을 수 없습니다."));

        methods.forEach(PayMethod::clearDefault);
        methods.forEach(PayMethod::clearFacePay);
        target.setAsDefault();
        target.setAsFacePay();

        payMethodRepository.saveAll(methods);
        return target;
    }

    /**
     * 페이스페이 결제수단 설정 = 대표 결제수단 설정 (통합)
     */
    @Transactional
    public PayMethod setFacePay(Long userNo, Long paymentMethodId) {
        return setDefault(userNo, paymentMethodId);
    }

    @Transactional
    public PayMethod updateFacePay(Long userNo, Long paymentMethodId, boolean enabled) {
        return enabled ? setFacePay(userNo, paymentMethodId) : clearFacePay(userNo, paymentMethodId);
    }

    @Transactional
    public PayMethod clearFacePay(Long userNo, Long paymentMethodId) {
        List<PayMethod> methods = payMethodRepository.findByUserNoAndIsActiveTrue(userNo);

        PayMethod target = methods.stream()
                .filter(method -> method.getPaymentMethodId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("결제 수단을 찾을 수 없습니다."));

        target.clearFacePay();
        payMethodRepository.save(target);
        return target;
    }
}