package com.ssafy.naeda.domain.point.repository;

import com.ssafy.naeda.domain.point.entity.PointOrder;
import com.ssafy.naeda.domain.point.entity.PointProduct;
import com.ssafy.naeda.domain.user.entity.User;
import com.ssafy.naeda.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PointOrderRepositoryTest {

    @Autowired
    private PointOrderRepository pointOrderRepository;

    @Autowired
    private PointProductRepository pointProductRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private PointProduct testProduct;
    private PointProduct testProduct2;

    @BeforeEach
    void setUp() {
        pointOrderRepository.deleteAll();
        pointProductRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .userId("test@test.com")
                .password("password123")
                .username("테스트유저")
                .residentNo("9901011")
                .phone("01012345678")
                .institutionCode("INST001")
                .build());

        testProduct = pointProductRepository.save(PointProduct.builder()
                .productName("아메리카노 쿠폰")
                .description("스타벅스 아메리카노")
                .category("카페")
                .pointPrice(3000L)
                .stockQuantity(100)
                .build());

        testProduct2 = pointProductRepository.save(PointProduct.builder()
                .productName("치킨 교환권")
                .description("BBQ 치킨")
                .category("음식")
                .pointPrice(15000L)
                .stockQuantity(50)
                .build());
    }

    private PointOrder buildOrder(Long userNo, Long productId) {
        return PointOrder.builder()
                .userNo(userNo)
                .productId(productId)
                .roadAddress("구미시 인동중앙로 100")
                .numberAddress("인동동 123-4")
                .build();
    }

    @Test
    @DisplayName("주문 저장 및 단건 조회")
    void saveAndFindById() {
        PointOrder saved = pointOrderRepository.save(
                buildOrder(testUser.getUserNo(), testProduct.getProductId()));

        PointOrder found = pointOrderRepository.findById(saved.getOrderId()).orElseThrow();
        assertThat(found.getUserNo()).isEqualTo(testUser.getUserNo());
        assertThat(found.getProductId()).isEqualTo(testProduct.getProductId());
        assertThat(found.getRoadAddress()).isEqualTo("구미시 인동중앙로 100");
        assertThat(found.getNumberAddress()).isEqualTo("인동동 123-4");
        assertThat(found.getOrderAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자별 주문 내역 조회")
    void findByUserNo() {
        pointOrderRepository.save(buildOrder(testUser.getUserNo(), testProduct.getProductId()));
        pointOrderRepository.save(buildOrder(testUser.getUserNo(), testProduct2.getProductId()));

        List<PointOrder> orders = pointOrderRepository.findByUserNo(testUser.getUserNo());
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(PointOrder::getUserNo)
                .containsOnly(testUser.getUserNo());
    }

    @Test
    @DisplayName("사용자별 주문 내역 - 없는 경우 빈 리스트")
    void findByUserNo_empty() {
        List<PointOrder> orders = pointOrderRepository.findByUserNo(99999L);
        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("상품별 주문 수 조회")
    void countByProductId() {
        pointOrderRepository.save(buildOrder(testUser.getUserNo(), testProduct.getProductId()));
        pointOrderRepository.save(buildOrder(testUser.getUserNo(), testProduct.getProductId()));
        pointOrderRepository.save(buildOrder(testUser.getUserNo(), testProduct2.getProductId()));

        Long count = pointOrderRepository.countByProductId(testProduct.getProductId());
        assertThat(count).isEqualTo(2);

        Long count2 = pointOrderRepository.countByProductId(testProduct2.getProductId());
        assertThat(count2).isEqualTo(1);
    }

    @Test
    @DisplayName("상품별 주문 수 - 주문 없는 경우 0")
    void countByProductId_zero() {
        Long count = pointOrderRepository.countByProductId(99999L);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("주문 삭제")
    void deleteOrder() {
        PointOrder saved = pointOrderRepository.save(
                buildOrder(testUser.getUserNo(), testProduct.getProductId()));
        Long id = saved.getOrderId();

        pointOrderRepository.deleteById(id);

        assertThat(pointOrderRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("주소 없이 주문 저장")
    void saveWithoutAddress() {
        PointOrder saved = pointOrderRepository.save(PointOrder.builder()
                .userNo(testUser.getUserNo())
                .productId(testProduct.getProductId())
                .build());

        PointOrder found = pointOrderRepository.findById(saved.getOrderId()).orElseThrow();
        assertThat(found.getRoadAddress()).isNull();
        assertThat(found.getNumberAddress()).isNull();
        assertThat(found.getOrderAt()).isNotNull();
    }
}
