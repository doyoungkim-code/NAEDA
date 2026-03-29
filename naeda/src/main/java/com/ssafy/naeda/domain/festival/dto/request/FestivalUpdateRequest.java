package com.ssafy.naeda.domain.festival.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class FestivalUpdateRequest {

    @NotBlank(message = "축제명은 필수입니다.")
    private String title;

    private String description;
    private String location;
    private String roadAddress;
    private String numberAddress;
    private Double latitude;
    private Double longitude;
    private String linkUrl;
    private String imageUrl;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;
}
