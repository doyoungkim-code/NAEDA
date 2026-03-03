package com.ssafy.naeda.domain.face.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchResponse {

    private boolean matched;
    private String bestUserId;
    private float similarity;
    private float threshold;
    private List<CandidateDto> candidates;
}
