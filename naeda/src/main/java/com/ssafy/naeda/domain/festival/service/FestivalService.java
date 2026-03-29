package com.ssafy.naeda.domain.festival.service;

import com.ssafy.naeda.domain.festival.dto.request.FestivalCreateRequest;
import com.ssafy.naeda.domain.festival.dto.request.FestivalUpdateRequest;
import com.ssafy.naeda.domain.festival.dto.response.FestivalResponse;
import com.ssafy.naeda.domain.festival.entity.Festival;
import com.ssafy.naeda.domain.festival.repository.FestivalRepository;
import com.ssafy.naeda.global.exception.BadRequestException;
import com.ssafy.naeda.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalService {

    private final FestivalRepository festivalRepository;

    @Transactional
    public FestivalResponse createFestival(FestivalCreateRequest request) {
        Festival festival = Festival.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .roadAddress(request.getRoadAddress())
                .numberAddress(request.getNumberAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .linkUrl(request.getLinkUrl())
                .imageUrl(request.getImageUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return FestivalResponse.from(festivalRepository.save(festival));
    }

    public FestivalResponse getFestival(Long festivalId) {
        Festival festival = findFestivalOrThrow(festivalId);
        return FestivalResponse.from(festival);
    }

    public List<FestivalResponse> getAllFestivals() {
        return festivalRepository.findAllByOrderByStartDateDesc()
                .stream()
                .map(FestivalResponse::from)
                .toList();
    }

    @Transactional
    public FestivalResponse updateFestival(Long festivalId, FestivalUpdateRequest request) {
        Festival festival = findFestivalOrThrow(festivalId);
        festival.update(
                request.getTitle(),
                request.getDescription(),
                request.getLocation(),
                request.getRoadAddress(),
                request.getNumberAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.getLinkUrl(),
                request.getImageUrl(),
                request.getStartDate(),
                request.getEndDate()
        );
        return FestivalResponse.from(festival);
    }

    @Transactional
    public void deleteFestival(Long festivalId) {
        Festival festival = findFestivalOrThrow(festivalId);
        festivalRepository.delete(festival);
    }

    @Transactional
    public Festival markAsNotified(Long festivalId) {
        Festival festival = findFestivalOrThrow(festivalId);
        if (Boolean.TRUE.equals(festival.getFcmNotified())) {
            throw new BadRequestException("이미 FCM 알림이 발송된 축제입니다.");
        }
        festival.markFcmNotified();
        return festival;
    }

    private Festival findFestivalOrThrow(Long festivalId) {
        return festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 축제입니다."));
    }
}
