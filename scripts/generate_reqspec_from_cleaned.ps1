$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$outFile = Join-Path $root "docs\요구명세서_상세_FE_BE_AI_INFRA_스터디포인트.xlsx"
$tmp = Join-Path $root ".tmp_reqspec_cleaned_xlsx"

if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
New-Item -ItemType Directory -Path $tmp | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tmp "_rels") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tmp "xl") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tmp "xl\_rels") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tmp "xl\worksheets") | Out-Null

function Escape-Xml([string]$s) {
  if ($null -eq $s) { return "" }
  return $s.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;").Replace('"', "&quot;").Replace("'", "&apos;")
}

function ColName([int]$n) {
  $name = ""
  while ($n -gt 0) {
    $m = ($n - 1) % 26
    $name = [char](65 + $m) + $name
    $n = [math]::Floor(($n - 1) / 26)
  }
  return $name
}

function Parse-TabBlock([string]$block) {
  $lines = $block -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 }
  $rows = @()
  foreach ($line in $lines) {
    $rows += ,($line -split "`t")
  }
  return $rows
}

function Is-Fib([int]$n) {
  if ($n -lt 0) { return $false }
  $a = 0
  $b = 1
  while ($a -lt $n) {
    $t = $a + $b
    $a = $b
    $b = $t
  }
  return ($a -eq $n)
}

function Next-Fib([int]$n) {
  if ($n -le 1) { return 1 }
  $a = 1
  $b = 2
  while ($b -lt $n) {
    $t = $a + $b
    $a = $b
    $b = $t
  }
  if ($b -eq $n) { return $n }
  return $b
}

function Normalize-StudyPoints([object[]]$rows) {
  if ($rows.Count -le 1) { return $rows }
  $sum = 0
  $sumRowIndex = -1

  for ($i = 1; $i -lt $rows.Count; $i++) {
    $row = $rows[$i]
    if ($row.Count -lt 9) { continue }
    $id = [string]$row[0]
    $pointStr = [string]$row[8]

    if ($id -like "*-SUM") {
      $sumRowIndex = $i
      continue
    }

    $p = 0
    if ([int]::TryParse($pointStr, [ref]$p)) {
      if (-not (Is-Fib $p)) {
        $p = Next-Fib $p
        $row[8] = [string]$p
      }
      $sum += $p
    }
  }

  if ($sumRowIndex -ge 1 -and $rows[$sumRowIndex].Count -ge 9) {
    $rows[$sumRowIndex][8] = [string]$sum
  }

  return $rows
}

function Write-SheetXml([string]$path, [object[]]$rows) {
  $sb = New-Object System.Text.StringBuilder
  [void]$sb.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
  [void]$sb.Append('<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">')
  [void]$sb.Append("<sheetData>")

  for ($r = 0; $r -lt $rows.Count; $r++) {
    $rowNum = $r + 1
    [void]$sb.Append(('<row r="{0}">' -f $rowNum))
    $cols = $rows[$r]
    for ($c = 0; $c -lt $cols.Count; $c++) {
      $cellRef = "$(ColName($c + 1))$rowNum"
      $v = Escape-Xml([string]$cols[$c])
      [void]$sb.Append(('<c r="{0}" t="inlineStr"><is><t>{1}</t></is></c>' -f $cellRef, $v))
    }
    [void]$sb.Append("</row>")
  }

  [void]$sb.Append("</sheetData></worksheet>")
  [System.IO.File]::WriteAllText($path, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))
}

$header = "ID`t도메인`t요구사항`t상세명세`t연계대상(화면/API/모듈)`t우선순위`t수용기준`t스터디주제`t스터디포인트"

$feData = @"
$header
FE-001	인증	로그인 화면 구성	아이디/비밀번호 입력, 유효성 검사, 오류 메시지 매핑, 토큰 저장 플로우 제공	사용자앱-로그인화면 / POST /auth/login	MUST	성공 시 메인 진입, 실패 시 원인별 안내 문구 표시	JWT 인증 흐름	4
FE-002	인증	회원가입/본인인증 화면	회원가입 입력 + 1원 인증 절차를 단계형 UI로 분리하고 상태값 유지	사용자앱-가입화면 / POST /auth/signup, /auth/verify-1won	MUST	1원 인증 완료 전 결제 메뉴 비활성화	본인확인 UX 설계	4
FE-003	뱅킹	계좌 목록 화면	보유 계좌, 잔액, 별칭, 계좌 유형을 카드형으로 표시	사용자앱-계좌목록 / GET /accounts	MUST	빈 데이터/로딩/오류 상태를 모두 제공	상태 기반 UI 렌더링	3
FE-004	뱅킹	계좌 상세 화면	선택 계좌의 거래 요약, 최근 거래 10건, 빠른 이체 버튼 제공	사용자앱-계좌상세 / GET /accounts/{id}	MUST	상세 진입 시 2초 내 핵심 정보 표시	지연 로딩 최적화	3
FE-005	뱅킹	이체 입력 화면	출금계좌/입금계좌/금액/메모 입력 및 실시간 유효성 검사	사용자앱-이체 / POST /transfers	MUST	유효성 오류가 있는 경우 전송 버튼 비활성화	폼 검증 패턴	3
FE-006	뱅킹	이체 확인/완료 화면	요청 데이터 재확인, 성공/실패 결과 및 영수증 공유 기능 제공	사용자앱-이체결과	MUST	거래번호와 처리시각이 항상 표시	거래 영수증 UX	2
FE-007	뱅킹	거래내역 화면	기간 필터, 유형 필터, 페이징 스크롤 제공	사용자앱-거래내역 / GET /transactions	MUST	중복/누락 없이 페이지네이션 동작	무한 스크롤 처리	3
FE-008	뱅킹	카드 결제내역 화면	카드 결제 건만 필터링하여 결제처/금액/시각 표시	사용자앱-카드내역 / GET /cards/transactions	SHOULD	필터 토글 즉시 목록 반영	클라이언트 필터링/서버 필터링	2
FE-009	FacePay	얼굴 등록 가이드 화면	다각도 촬영 가이드(정면/좌/우/상/하)와 품질 조건 안내	사용자앱-얼굴등록	MUST	필수 각도 미충족 시 등록 버튼 비활성화	CameraX 캡처 UX	4
FE-010	FacePay	얼굴 등록 처리 화면	촬영 진행률, 업로드 상태, 실패 재시도 UX 제공	사용자앱-얼굴등록업로드 / POST /face/register	MUST	원본 이미지 저장 안내 없음, 벡터 등록 성공 메시지 노출	민감정보 고지 UX	3
FE-011	단말기	결제수단 선택 화면	카드/카카오/삼성/FacePay 버튼 구조와 POS 금액 표시	단말기앱-결제선택	MUST	FacePay 선택 시 카메라 단계 이동	결제수단 확장 가능한 UI 구조	2
FE-012	단말기	Liveness 미션 화면	눈 깜빡임/고개 방향 지시를 순차 표시하고 진행률 표시	단말기앱-CameraX / AI-Liveness	MUST	Liveness 실패 사유를 사용자 친화 문구로 제공	MediaPipe 동작 이해	4
FE-013	단말기	얼굴 매칭 대기 화면	매칭 중 로딩/취소/재시도 버튼 제공	단말기앱-매칭대기 / POST /face/match	MUST	서버 타임아웃 시 재시도 경로 제공	네트워크 예외 UX	3
FE-014	인증	RBA 추가인증 화면	금액/유사도 구간별 전화번호 4자리 확인, 전자서명, 결제 차단 안내 화면 분기	단말기앱-추가인증 / POST /payments/verify	MUST	정책 조건에 맞는 인증 컴포넌트만 노출	조건 분기 UI 설계	5
FE-015	뱅킹	결제 한도 설정 화면	1회 한도/1일 한도 설정, 변경 이력 확인, 초과 시 차단 안내 제공	사용자앱-한도설정 / GET, PATCH /payments/limits	MUST	한도 초과 결제 시 결제 시도 즉시 차단 안내 노출	한도 정책 UX	4
FE-016	알림	결제 완료 FCM 알림 화면	결제 완료 알림 수신, 거래 요약 표시, 알림 탭 시 상세 이동 제공	사용자앱-알림센터 / FCM	MUST	결제 성공 건에 대해 중복 없이 1회 알림 수신	FCM 딥링크 처리	4
FE-017	알림	축제/이벤트 FCM 알림 화면	축제 예정 알림 메시지, 일정 카드, 상세 안내 링크 제공	사용자앱-이벤트알림 / FCM	SHOULD	공지성 알림이 사용자 수신 동의 상태에 맞게 발송됨	알림 템플릿 설계	3
FE-018	포인트	포인트 지갑 화면	보유 포인트, 적립/사용 내역, 지역 전용 사용처 안내 표시	사용자앱-포인트지갑 / GET /points	MUST	적립/차감 후 잔액이 즉시 갱신	포인트 원장 UI	3
FE-019	포인트	포인트 사용 결제 UX	결제 시 포인트 사용 여부 선택, 차감 금액 미리보기 제공	사용자앱-결제옵션 / POST /points/redeem	MUST	사용 가능 포인트 초과 입력 방지	금융 계산 UI	3
FE-020	추천	구미 추천 리스트 화면	카테고리(맛집/카페/관광지) 필터와 추천 점수 라벨 제공	사용자앱-추천목록 / GET /local/recommendations	SHOULD	필터 변경 후 1초 내 목록 재렌더링	추천 UX 기초	2
FE-021	소비분석	소비 분석 대시보드	월별 카테고리 차트, 전월 대비 증감, 절약 인사이트 카드 제공	사용자앱-리포트 / GET /insights	SHOULD	월 전환 시 지표 계산 오차 없이 표시	데이터 시각화	4
FE-022	공통	오류/오프라인 공통 처리	API 장애, 카메라 권한 거부, 네트워크 불안정에 대한 공통 모달과 재시도 제공	사용자앱/단말기앱 공통	MUST	치명 장애에서 앱이 중단되지 않음	에러 핸들링 패턴	3
FE-SUM	합계	스터디포인트 합계	FE 파트 학습 난이도 총량	-	-	-	-	71
"@

$beData = @"
$header
BE-001	인증	회원가입 API	사용자 생성, 약관 동의, 중복 검사, 계정 활성화 상태 관리	POST /auth/signup	MUST	중복 계정 차단 및 성공 시 사용자ID 반환	Spring Security 기본	3
BE-002	인증	로그인/토큰 API	로그인 검증, JWT 발급, refresh 토큰 재발급 제공	POST /auth/login, /auth/refresh	MUST	만료 토큰 재발급 정책 정상 동작	JWT/Refresh 전략	4
BE-003	인증	1원 인증 API	인증 요청 생성, 입금 식별값 검증, 사용자 검증 상태 반영	POST /auth/verify-1won	MUST	검증 실패 사유 코드화	외부 인증 API 연계	4
BE-004	뱅킹	계좌 조회 API	사용자별 계좌 목록/상세 조회와 마스킹 규칙 제공	GET /accounts, /accounts/{id}	MUST	권한 없는 계좌 접근 차단	권한 기반 데이터 접근	3
BE-005	뱅킹	이체 API	잔액 확인, 계좌 검증, 이체 요청, 실패 롤백 처리	POST /transfers	MUST	원자성 보장 및 거래번호 생성	트랜잭션 처리	5
BE-006	뱅킹	거래내역 API	기간/유형/페이지 조건 조회와 정렬 기준 제공	GET /transactions	MUST	정렬/필터 조합 테스트 통과	쿼리 최적화	3
BE-007	뱅킹	카드내역 API	카드 승인/취소 포함 결제 이력 조회 제공	GET /cards/transactions	SHOULD	취소 거래 상태가 명확히 구분됨	결제 상태 모델링	3
BE-008	FacePay	얼굴 등록 중계 API	AI 서버 등록 결과 수신 후 사용자 얼굴등록 상태 저장	POST /face/register	MUST	원본 이미지 저장 경로 없음 보장	민감정보 저장 정책	4
BE-009	FacePay	얼굴 매칭 중계 API	단말기 입력 이미지 기반 매칭 요청/결과 표준화	POST /face/match	MUST	응답에 유사도/판정코드 포함	AI 추론 API 계약	4
BE-010	결제	FacePay 결제 오케스트레이션	Liveness->매칭->RBA->카드승인 순서로 실행하는 상태머신 구현	POST /payments/facepay	MUST	순서 위반 시 즉시 실패 처리	상태머신 설계	5
BE-011	RBA	금액 기반 정책 엔진	5만원 미만은 얼굴만, 5만원 이상은 얼굴+전자서명으로 정책 분기	Payment Policy Module	MUST	금액 경계값(49,999/50,000) 테스트 통과	룰 엔진 설계	5
BE-012	RBA	유사도 기반 단계 인증	유사도 80 이상 즉시 결제, 70~79 전화번호 4자리 검증, 완전 실패 시 계정 유무 확인 후 차단 처리	POST /payments/verify	MUST	구간별 인증요소와 차단 정책이 정확히 강제됨	위험도 기반 인증 설계	5
BE-013	결제	결제 한도 설정 API	사용자별 1회/1일 결제 한도 조회 및 변경, 초과 여부 검증 로직 제공	GET, PATCH /payments/limits	MUST	한도 초과 거래는 승인 요청 전에 차단됨	한도 정책 백엔드	4
BE-014	RBA	전화번호 4자리 검증 API	중간 유사도 구간에서 마스킹 번호 기반 본인 확인 제공	POST /payments/verify-phone4	MUST	입력 횟수 제한 및 감사 로그 저장	추가 본인확인 로직	3
BE-015	RBA	전자서명 검증 API	5만원 이상 결제의 서명 이미지/벡터 검증 및 저장	POST /payments/signature	MUST	서명 누락 시 결제 진행 불가	전자서명 데이터 처리	4
BE-016	알림	결제/축제 FCM 발송 API	결제 완료 및 축제 예정 알림을 토픽/사용자 단위로 발송하고 이력 저장	POST /notifications/fcm/send	MUST	발송 성공/실패 상태와 재시도 이력이 저장됨	FCM 서버 연동	4
BE-017	FDS	FDS 1차 룰 평가 API	심야/지역/빈도 규칙 평가와 위험 이벤트 생성	POST /fds/rules/evaluate	SHOULD	룰 트리거 근거 필드 저장	탐지 룰 설계	4
BE-018	FDS	FDS 점수 대응 처리	0~30 로그, 30~60 알림, 60~80 정지, 80~100 차단 실행	POST /fds/act	MUST	점수 구간 경계값 테스트 통과	정책 자동화	4
BE-019	포인트	포인트 적립 API	구미 매장 10% 적립, 일반 매장 기본 적립 정책 분리	POST /points/accrue	MUST	적립 계산 오차 0원	정산 로직 구현	4
BE-020	포인트	포인트 사용/환전 API	결제 차감, 지역 전용 사용처 검증, 지역화폐 전환 처리	POST /points/redeem, /points/convert	MUST	지역 외 사용 시 차단 처리	지역화폐 모델링	5
BE-021	추천	구미 추천 API	카테고리/위치 기반 목록 반환 및 추천 점수 포함	GET /local/recommendations	SHOULD	응답 1초 내(캐시 기준)	추천 API 설계	3
BE-022	소비분석	소비 리포트 API	카테고리 집계, 월간 변화율, 인사이트 문구 반환	GET /insights	MUST	동일 데이터 입력 시 동일 결과 반환	집계 파이프라인 설계	4
BE-023	공통	감사 로그/추적ID	결제/인증/FDS 이벤트에 traceId와 actor 정보 저장	Log Module	MUST	거래 단위 추적이 가능	관측성 설계	3
BE-024	공통	외부 API 장애 대응	SSAFY API 타임아웃/재시도/서킷브레이커 적용	Integration Module	MUST	외부 장애 시 내부 서비스 연쇄 실패 방지	회복탄력성 패턴	4
BE-SUM	합계	스터디포인트 합계	BE 파트 학습 난이도 총량	-	-	-	-	94
"@

$aiData = @"
$header
AI-001	얼굴인식	얼굴 등록 임베딩 생성	다각도 촬영 이미지에서 128차원 벡터 생성 및 품질 체크	FastAPI /face/register	MUST	품질 기준 미달 샘플은 등록 거부	face_recognition 임베딩	5
AI-002	얼굴인식	얼굴 벡터 암호화 저장	벡터 저장 전 암호화 및 키 관리 정책 적용	FastAPI + PostgreSQL	MUST	원본 이미지 미저장 정책 검증 통과	벡터 보안 저장	5
AI-003	Liveness	깜빡임(EAR) 검증	연속 프레임 기반 눈 깜빡임 임계치 판정	Liveness Module	MUST	정지 이미지/영상 재생 공격 탐지	Mediapipe EAR	4
AI-004	Liveness	고개 방향 검증	랜덤 지시(좌/우/상/하)에 대한 실시간 방향 추적	Liveness Module	MUST	지시 불일치 시 실패 반환	Head Pose Estimation	4
AI-005	매칭	실시간 얼굴 매칭 추론	입력 얼굴과 저장 벡터의 거리 계산 및 유사도 점수 산출	FastAPI /face/match	MUST	응답에 유사도(0~100) 포함	거리 기반 분류	4
AI-006	매칭	임계값(tolerance) 튜닝	운영 데이터 기반 FAR/FRR 균형점을 찾아 임계값 갱신	Model Ops	SHOULD	튜닝 전후 정확도 비교 리포트 생성	모델 평가 지표	5
AI-007	매칭	RBA 연계 판정코드 생성	유사도 구간을 정책 엔진이 해석 가능한 코드로 매핑	FastAPI -> BE	MUST	구간 경계값 79/80 오분류 없음	모델-정책 인터페이스	4
AI-008	FDS	1차 규칙 피처 생성	거래 시간/지역/빈도 피처 전처리 및 룰엔진 입력 생성	FDS Preprocess	SHOULD	누락 피처 없이 규칙평가 가능	피처 엔지니어링	3
AI-009	FDS	Isolation Forest 학습	사용자별 정상 패턴 학습 및 이상치 분리 모델 생성	FastAPI /fds/train	SHOULD	학습 실패 시 이전 모델 자동 유지	비지도 이상탐지	5
AI-010	FDS	이상 점수 산출 API	실시간 거래 입력의 anomaly score 계산 및 0~100 정규화	FastAPI /fds/score	MUST	동일 입력 재현성 보장	스코어링 파이프라인	4
AI-011	FDS	점수 구간 추천 액션	점수별 대응(로그/알림/정지/차단) 추천값 반환	FastAPI -> BE	SHOULD	구간별 액션 누락 없음	리스크 매핑 로직	3
AI-012	소비분석	카테고리 분류 모델	거래처/메모 기반 소비 카테고리 자동 분류	/insights/classify	SHOULD	수동 라벨 대비 정확도 기준 충족	NLP/룰 기반 분류	4
AI-013	소비분석	월간 소비 인사이트 생성	카테고리 증감/이상 소비 패턴 문장 생성	/insights/generate	MUST	과장/오류 문구 없이 수치 일치	데이터 기반 NLG	4
AI-014	추천	콘텐츠 기반 추천 점수 계산	사용자 선호 벡터와 매장 특성 벡터 유사도 계산	/recommend/score	SHOULD	Top-N 추천 품질 검증	코사인 유사도 추천	4
AI-015	추천	지역 가중치 적용	구미 소상공인 매장에 지역 가중치 반영하여 순위 재정렬	/recommend/rerank	MUST	가중치 적용 전후 순위 추적 가능	랭킹 재정렬	3
AI-016	MLOps	모델 버전 관리	얼굴/FDS/추천 모델 버전, 학습일, 지표 메타데이터 관리	Model Registry	SHOULD	문제 발생 시 즉시 롤백 가능	MLOps 기초	4
AI-017	MLOps	추론 성능 모니터링	응답시간, 실패율, 드리프트 지표 수집 및 경고	Monitoring	MUST	임계치 초과 시 운영 경고 발송	모델 모니터링	4
AI-018	보안	AI 입력 데이터 최소화	불필요 PII 제거 후 추론 파이프라인 전달	Data Governance	MUST	민감정보 과수집 점검 통과	AI 데이터 거버넌스	3
AI-019	안정성	AI 서버 폴백 전략	AI 장애 시 BE 룰 기반 최소 기능 제공	Fallback Strategy	MUST	AI 장애에서도 결제 핵심 경로 유지	서비스 폴백 설계	4
AI-SUM	합계	스터디포인트 합계	AI 파트 학습 난이도 총량	-	-	-	-	79
"@

$infraData = @"
$header
INFRA-001	아키텍처	MSA 배포 구조 구성	사용자앱/단말기앱, Spring, FastAPI, DB 계층 분리 배포	전체 인프라	MUST	서비스 간 독립 배포 가능	MSA 토폴로지 설계	4
INFRA-002	네트워크	서비스 통신 경로 설계	앱->BE, BE->AI, BE->SSAFY API 네트워크 경로와 방화벽 규칙 설정	API Gateway/Firewall	MUST	비허용 포트 접근 차단	네트워크 보안 기초	4
INFRA-003	데이터	PostgreSQL 운영 구성	트랜잭션/인덱스/백업 정책 및 복구 시나리오 수립	DB Cluster	MUST	RPO/RTO 목표 문서화	데이터베이스 운영	4
INFRA-004	데이터	Redis 운영 구성	세션/캐시/일시 상태 저장 및 TTL 정책 적용	Redis	MUST	만료 정책 미설정 키 0건 유지	캐시 전략	3
INFRA-005	보안	JWT 보안 정책	서명키 관리, 만료 정책, 토큰 탈취 대응 룰 적용	Auth Infra	MUST	취약 토큰 재사용 탐지 가능	인증 보안 운영	4
INFRA-006	보안	비밀정보 관리	DB 비밀번호, API 키, 암호화 키를 환경별로 분리 관리	Secret Store	MUST	코드 저장소 내 평문 키 0건	시크릿 매니지먼트	4
INFRA-007	배포	CI/CD 파이프라인	빌드-테스트-배포 자동화와 실패 롤백 스텝 구성	GitHub Actions/Jenkins	SHOULD	배포 실패 시 자동 중단/알림	파이프라인 설계	4
INFRA-008	관측성	로그 수집/검색	BE/AI/인프라 로그 중앙수집, traceId 기반 검색 제공	Log Stack	MUST	장애 발생 시 10분 내 원인 추적 가능	로깅 아키텍처	3
INFRA-009	관측성	메트릭/알림 운영	CPU/Mem/API 지연/FDS 오류율 대시보드 및 알림 룰 구축	Monitoring Stack	MUST	임계치 초과 알림 누락 없음	모니터링 체계	3
INFRA-010	회복탄력성	외부 API 회복 전략	SSAFY API 장애 대비 재시도, 서킷브레이커, degrade 응답 제공	Integration Infra	MUST	외부 장애에서도 핵심 거래 흐름 유지	Resilience 패턴	5
INFRA-011	회복탄력성	서비스 헬스체크	서비스별 readiness/liveness 및 자동 재기동 정책 구성	Orchestration	SHOULD	비정상 인스턴스 자동 교체	헬스체크 운영	3
INFRA-012	모바일연계	FCM 테스트 인프라	결제 완료/축제 공지 알림 시나리오 테스트 환경과 전송 로그 수집 구축	QA Infra	MUST	알림 누락/중복 원인을 환경별로 구분 가능	FCM QA 체계	4
INFRA-013	모바일연계	FCM 운영 구성	푸시 인증키, 토픽/토큰 관리, 전송 실패 재시도 구성	Push Infra	SHOULD	푸시 실패율 기준치 이내 유지	푸시 인프라 운영	3
INFRA-014	데이터보호	민감데이터 암호화 정책	얼굴벡터/PIN 해시/전송 TLS 정책 준수 여부 점검	Security Baseline	MUST	평문 민감정보 저장 0건	암호화 정책 적용	4
INFRA-015	개발생산성	Mock 서버 운영	외부 API 지연 대비 Mock 서버 병렬 운영 및 전환 스위치 제공	Dev/Test Infra	MUST	외부 API 없이도 핵심 기능 개발 가능	Mock/Stub 운영	3
INFRA-016	품질	성능/부하 테스트	결제 피크 트래픽, AI 추론 동시성, DB 부하 테스트 수행	Perf Test Infra	SHOULD	SLA 목표치 미달 지점 식별	성능 테스트 설계	4
INFRA-017	품질	보안 점검 자동화	의존성 취약점 점검, 이미지 스캔, SAST 기본 파이프라인 연동	SecOps	SHOULD	고위험 취약점 배포 차단	SecOps 기초	4
INFRA-SUM	합계	스터디포인트 합계	INFRA 파트 학습 난이도 총량	-	-	-	-	66
"@

$feRows = Parse-TabBlock $feData
$beRows = Parse-TabBlock $beData
$aiRows = Parse-TabBlock $aiData
$infraRows = Parse-TabBlock $infraData

$feRows = Normalize-StudyPoints $feRows
$beRows = Normalize-StudyPoints $beRows
$aiRows = Normalize-StudyPoints $aiRows
$infraRows = Normalize-StudyPoints $infraRows

Write-SheetXml (Join-Path $tmp "xl\worksheets\sheet1.xml") $feRows
Write-SheetXml (Join-Path $tmp "xl\worksheets\sheet2.xml") $beRows
Write-SheetXml (Join-Path $tmp "xl\worksheets\sheet3.xml") $aiRows
Write-SheetXml (Join-Path $tmp "xl\worksheets\sheet4.xml") $infraRows

$contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>
'@
[System.IO.File]::WriteAllText((Join-Path $tmp "[Content_Types].xml"), $contentTypes.Trim(), (New-Object System.Text.UTF8Encoding($false)))

$rels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
'@
[System.IO.File]::WriteAllText((Join-Path $tmp "_rels\.rels"), $rels.Trim(), (New-Object System.Text.UTF8Encoding($false)))

$workbook = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="FE" sheetId="1" r:id="rId1"/>
    <sheet name="BE" sheetId="2" r:id="rId2"/>
    <sheet name="AI" sheetId="3" r:id="rId3"/>
    <sheet name="INFRA" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>
'@
[System.IO.File]::WriteAllText((Join-Path $tmp "xl\workbook.xml"), $workbook.Trim(), (New-Object System.Text.UTF8Encoding($false)))

$wbRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
'@
[System.IO.File]::WriteAllText((Join-Path $tmp "xl\_rels\workbook.xml.rels"), $wbRels.Trim(), (New-Object System.Text.UTF8Encoding($false)))

$styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>
'@
[System.IO.File]::WriteAllText((Join-Path $tmp "xl\styles.xml"), $styles.Trim(), (New-Object System.Text.UTF8Encoding($false)))

$zipPath = Join-Path $root "docs\요구명세서_상세_FE_BE_AI_INFRA_스터디포인트.zip"
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
if (Test-Path $outFile) {
  try {
    Remove-Item -Force $outFile
  } catch {
    $outFile = Join-Path $root "docs\요구명세서_상세_FE_BE_AI_INFRA_스터디포인트_v2.xlsx"
  }
}
Compress-Archive -Path (Join-Path $tmp "*") -DestinationPath $zipPath -Force
Move-Item -Path $zipPath -Destination $outFile
Remove-Item -Recurse -Force $tmp

Write-Output "CREATED: $outFile"
