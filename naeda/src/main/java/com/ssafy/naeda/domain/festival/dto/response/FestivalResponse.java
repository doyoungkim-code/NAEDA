package com.ssafy.naeda.domain.festival.dto.response;

import com.ssafy.naeda.domain.festival.entity.Festival;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class FestivalResponse {

    private Long festivalId;
    private String title;
    private String description;
    private String location;
    private String roadAddress;
    private String numberAddress;
    private Double latitude;
    private Double longitude;
    private String linkUrl;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean fcmNotified;
    private LocalDateTime created;

    public static FestivalResponse from(Festival festival) {
        return FestivalResponse.builder()
                .festivalId(festival.getFestivalId())
                .title(festival.getTitle())
                .description(festival.getDescription())
                .location(festival.getLocation())
                .roadAddress(festival.getRoadAddress())
                .numberAddress(festival.getNumberAddress())
                .latitude(festival.getLatitude())
                .longitude(festival.getLongitude())
                .linkUrl(festival.getLinkUrl())
                .imageUrl(festival.getImageUrl())
                .startDate(festival.getStartDate())
                .endDate(festival.getEndDate())
                .fcmNotified(festival.getFcmNotified())
                .created(festival.getCreated())
                .build();
    }
}
