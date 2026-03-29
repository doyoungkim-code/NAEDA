//package com.ssafy.naeda.domain.payment.service;
//
//import com.ssafy.naeda.domain.payment.entity.PaymentMethod;
//import com.ssafy.naeda.domain.payment.repository.PaymentMethodRepository;
//import com.ssafy.naeda.global.exception.BadRequestException;
//import com.ssafy.naeda.global.exception.NotFoundException;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class PaymentMethodService {
//
//    private final PaymentMethodRepository paymentMethodRepository;
//
//    /**
//     * 사용자의 결제 수단 목록 조회.
//     */
//    public List<PaymentMethod> getPaymentMethods(Long userNo) {
//        return paymentMethodRepository.findByUserNoAndIsActiveTrue(userNo);
//    }
//
//    /**
//     * 페이스페이 결제 수단 지정.
//     * 기존 isFacePay=true 항목을 해제하고, 지정된 결제 수단에 isFacePay=true 설정.
//     */
//    @Transactional
//    public void setFacePayMethod(Long userNo, Long paymentMethodId) {
//        PaymentMethod target = paymentMethodRepository.findById(paymentMethodId)
//                .orElseThrow(() -> new NotFoundException("존재하지 않는 결제 수단입니다."));
//
//        if (!target.getUserNo().equals(userNo)) {
//            throw new BadRequestException("본인의 결제 수단만 설정할 수 있습니다.");
//        }
//
//        // 기존 페이스페이 수단 해제
//        paymentMethodRepository.findByUserNoAndIsFacePayTrueAndIsActiveTrue(userNo)
//                .ifPresent(PaymentMethod::clearFacePay);
//
//        target.setAsFacePay();
//    }
//}
