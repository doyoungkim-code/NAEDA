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

    @Transactional
    public PayMethod setDefault(Long userNo, Long paymentMethodId) {
        List<PayMethod> methods = payMethodRepository.findByUserNoAndIsActiveTrue(userNo);

        PayMethod target = methods.stream()
                .filter(method -> method.getPaymentMethodId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("결제 수단을 찾을 수 없습니다."));

        methods.forEach(PayMethod::clearDefault);
        target.setAsDefault();

        payMethodRepository.saveAll(methods);
        return target;
    }

    @Transactional
    public PayMethod setFacePay(Long userNo, Long paymentMethodId) {
        List<PayMethod> methods = payMethodRepository.findByUserNoAndIsActiveTrue(userNo);

        PayMethod target = methods.stream()
                .filter(method -> method.getPaymentMethodId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("결제 수단을 찾을 수 없습니다."));

        methods.forEach(PayMethod::clearDefault);
        methods.forEach(PayMethod::clearFacePay);
        target.setAsFacePay();

        payMethodRepository.saveAll(methods);
        return target;
    }
}
