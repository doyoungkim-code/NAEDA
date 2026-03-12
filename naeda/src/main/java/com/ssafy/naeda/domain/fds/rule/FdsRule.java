package com.ssafy.naeda.domain.fds.rule;

import com.ssafy.naeda.domain.fds.dto.request.FdsEvaluationRequest;
import com.ssafy.naeda.domain.fds.entity.FdsRuleName;

/**
 * FDS 개별 규칙 인터페이스 (Strategy 패턴).
 *
 * 각 구현체는 @Component로 등록하면
 * FdsEngine에서 List<FdsRule>로 자동 주입받아 순회 실행한다.
 *
 * 규칙 추가 시 이 인터페이스만 구현하면 되므로 OCP(개방-폐쇄 원칙) 준수.
 */
public interface FdsRule {

    /**
     * 이 규칙의 이름.
     */
    FdsRuleName ruleName();

    /**
     * 규칙을 평가하고 이상 점수를 반환한다.
     *
     * @param request 평가 입력 데이터
     * @return 이상 점수 (0이면 정상, 양수면 이상 감지). 각 룰은 0~33 범위를 권장.
     *         (3개 룰 × 33 ≈ 99, 최대 100으로 cap)
     */
    int evaluate(FdsEvaluationRequest request);
}