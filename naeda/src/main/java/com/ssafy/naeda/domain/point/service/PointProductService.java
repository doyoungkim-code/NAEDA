package com.ssafy.naeda.domain.point.service;

import com.ssafy.naeda.domain.point.dto.request.PointProductCreateRequest;
import com.ssafy.naeda.domain.point.dto.request.PointProductUpdateRequest;
import com.ssafy.naeda.domain.point.dto.response.PointProductResponse;
import com.ssafy.naeda.domain.point.entity.PointProduct;
import com.ssafy.naeda.domain.point.entity.PointProductStatus;
import com.ssafy.naeda.domain.point.repository.PointProductRepository;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointProductService {

    private final PointProductRepository pointProductRepository;

    @Transactional
    public PointProductResponse createProduct(PointProductCreateRequest request) {
        PointProduct product = request.toEntity();
        PointProduct saved = pointProductRepository.save(product);
        return PointProductResponse.from(saved);
    }

    public PointProductResponse getProduct(Long productId) {
        PointProduct product = findByIdOrThrow(productId);
        return PointProductResponse.from(product);
    }

    public List<PointProductResponse> getAllProducts() {
        return pointProductRepository.findAll().stream()
                .map(PointProductResponse::from)
                .toList();
    }

    public List<PointProductResponse> getAvailableProducts(String category, String keyword) {
        List<PointProduct> products;

        if (keyword != null && category != null) {
            products = pointProductRepository.findByProductNameContainingAndCategoryAndStatus(keyword, category, PointProductStatus.ON_SALE);
        } else if (keyword != null) {
            products = pointProductRepository.findByProductNameContainingAndStatus(keyword, PointProductStatus.ON_SALE);
        } else if (category != null) {
            products = pointProductRepository.findByCategoryAndStatus(category, PointProductStatus.ON_SALE);
        } else {
            products = pointProductRepository.findByStatus(PointProductStatus.ON_SALE);
        }

        return products.stream()
                .filter(p -> p.getStockQuantity() > 0)
                .map(PointProductResponse::from)
                .toList();
    }

    @Transactional
    public PointProductResponse updateProduct(Long productId, PointProductUpdateRequest request) {
        PointProduct product = findByIdOrThrow(productId);
        product.update(
                request.getProductName(),
                request.getDescription(),
                request.getCategory(),
                request.getImageUrl(),
                request.getPointPrice(),
                request.getStockQuantity(),
                request.getStatus(),
                request.getStartsAt(),
                request.getEndsAt()
        );
        return PointProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        PointProduct product = findByIdOrThrow(productId);
        pointProductRepository.delete(product);
    }

    private PointProduct findByIdOrThrow(Long productId) {
        return pointProductRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("포인트 상품을 찾을 수 없습니다. id=" + productId));
    }
}