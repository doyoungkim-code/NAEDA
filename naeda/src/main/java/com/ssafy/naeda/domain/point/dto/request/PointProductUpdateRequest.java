package com.ssafy.naeda.domain.point.dto.request;

import com.ssafy.naeda.domain.point.entity.PointProductStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointProductUpdateRequest {

    @NotBlank
    @Size(max = 120)
    private String productName;

    @Size(max = 500)
    private String description;

    @Size(max = 50)
    private String category;

    @Size(max = 500)
    private String imageUrl;

    @NotNull
    @Positive
    @Max(100_000_000)
    private Long pointPrice;

    @NotNull
    @PositiveOrZero
    private Integer stockQuantity;

    @NotNull
    private PointProductStatus status;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
}
