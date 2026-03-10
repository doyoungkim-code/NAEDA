# AI-015 모델 버전 관리

## 목적

- 현재 운영 중인 FDS 규칙/피처/모델 버전을 추적 가능하게 만든다.
- FE 없이 BE와 AI만으로 버전 조회와 운영 상태 확인이 가능하도록 한다.
- 향후 Isolation Forest 같은 실제 모델이 붙어도 같은 구조를 재사용할 수 있게 한다.

## 이번 작업 범위

현재 FDS는 규칙 기반 중심이므로, 이번 버전 관리도 최소 범위로 구현했다.

- AI
  - 현재 운영 버전 메타데이터를 설정값으로 관리
  - 내부 버전 조회 API 제공
- BE
  - AI 버전 정보를 조회해 내부 API로 정리해서 반환
  - 규칙 버전 fallback 값을 별도 설정으로 유지

## 버전 필드

- `featureVersion`
  - 현재 FDS 입력 피처 스키마 버전
- `ruleVersion`
  - 현재 규칙 엔진 버전
- `modelVersion`
  - 현재 운영 모델 버전
- `algorithm`
  - 현재 사용 중인 판단 방식
- `artifactPath`
  - 모델 아티팩트 경로
- `updatedAt`
  - 마지막 갱신 시각

현재 기본값은 아래와 같다.

- `featureVersion = fds-feature-v1`
- `ruleVersion = fds-rule-v1`
- `modelVersion = rule-only-v1`
- `algorithm = RULE_ENGINE`

즉 현재는 학습 모델이 아니라 규칙 엔진 기반 운영 상태를 명시적으로 버전으로 관리한다.

## AI 변경 사항

### 설정값 추가

`AI/app/core/config.py`

- `ai_feature_version`
- `ai_rule_version`
- `ai_model_version`
- `ai_model_algorithm`
- `ai_model_artifact_path`
- `ai_model_updated_at`

### 내부 API 추가

`GET /internal/v1/model/version`

응답 예시:

```json
{
  "featureVersion": "fds-feature-v1",
  "ruleVersion": "fds-rule-v1",
  "modelVersion": "rule-only-v1",
  "algorithm": "RULE_ENGINE",
  "artifactPath": "N/A",
  "updatedAt": "2026-03-09T00:00:00"
}
```

## BE 변경 사항

### 설정값 추가

`naeda/src/main/resources/application.yaml`

```yaml
fds:
  version:
    feature: fds-feature-v1
    rule: fds-rule-v1
```

### 내부 API 추가

`GET /api/internal/fds/version`

동작:

1. BE가 AI 내부 버전 API를 호출
2. AI 응답을 받아 현재 운영 버전으로 정리
3. AI 응답이 비어 있으면 BE 설정값 기준 fallback 적용

응답 예시:

```json
{
  "featureVersion": "fds-feature-v1",
  "ruleVersion": "fds-rule-v1",
  "modelVersion": "rule-only-v1",
  "algorithm": "RULE_ENGINE",
  "artifactPath": "N/A",
  "updatedAt": "2026-03-09T00:00:00"
}
```

## 기대 효과

- 현재 운영 중인 FDS 버전을 바로 확인 가능
- 규칙 기반 운영 상태도 버전 개념으로 추적 가능
- 나중에 실제 모델 학습/배포가 들어와도 같은 스키마 유지 가능

## 후속 확장

- 모델 파일 실제 경로 연동
- 학습 데이터 기간(`trainedFrom`, `trainedTo`) 추가
- 버전 이력 테이블 저장
- 특정 버전 롤백 API 또는 관리 기능 추가
