package com.ssafy.naeda.domain.rba.service;

import com.ssafy.naeda.domain.rba.dto.AuthLevel;

public interface RbaPolicy {
    AuthLevel evaluate(long amount, double similarity);
}