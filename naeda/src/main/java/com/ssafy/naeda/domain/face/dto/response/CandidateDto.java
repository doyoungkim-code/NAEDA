package com.ssafy.naeda.domain.face.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CandidateDto {
    private String userId;
    private String pose;
    private float similarity;
}
