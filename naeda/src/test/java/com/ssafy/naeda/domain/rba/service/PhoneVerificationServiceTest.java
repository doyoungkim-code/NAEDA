package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PhoneVerificationService phoneVerificationService;

    @Test
    @DisplayName("전화번호 가운데 4자리가 일치하면 true를 반환한다")
    void verify_returnsTrueWhenDigitsMatch() throws Exception {
        User user = buildUser(18L, "010-1234-5678");
        given(userRepository.findByUserNo(18L)).willReturn(Optional.of(user));

        boolean verified = phoneVerificationService.verify(18L, "1234");

        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("전화번호 가운데 4자리가 다르면 false를 반환한다")
    void verify_returnsFalseWhenDigitsMismatch() throws Exception {
        User user = buildUser(18L, "01012345678");
        given(userRepository.findByUserNo(18L)).willReturn(Optional.of(user));

        boolean verified = phoneVerificationService.verify(18L, "9999");

        assertThat(verified).isFalse();
    }

    private User buildUser(Long userNo, String phone) throws Exception {
        User user = User.builder()
                .userId("user-18")
                .password("encoded")
                .username("테스트")
                .residentNo("9001011")
                .phone(phone)
                .institutionCode("001")
                .build();
        Field field = User.class.getDeclaredField("userNo");
        field.setAccessible(true);
        field.set(user, userNo);
        return user;
    }
}
