# 내다(NAEDA) 사용자 앱 아키텍처 설계서

## 1. 아키텍처 패턴: MVVM + Clean Architecture (경량)

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│  Jetpack Compose Screens + Navigation                    │
│  (화면을 그리는 곳 — 버튼, 텍스트, 카메라 미리보기 등)       │
└───────────────────────┬─────────────────────────────────┘
                        │ State (상태) 관찰
                        ▼
┌─────────────────────────────────────────────────────────┐
│                   ViewModel Layer                         │
│  각 화면마다 ViewModel 1개                                │
│  (화면에 보여줄 데이터를 가공하고, 사용자 액션을 처리)        │
└───────────────────────┬─────────────────────────────────┘
                        │ 데이터 요청/응답
                        ▼
┌─────────────────────────────────────────────────────────┐
│                  Repository Layer                         │
│  데이터 출처를 하나로 통합 (Single Source of Truth)         │
│  (서버 API? 로컬 DB? 알아서 판단)                          │
└──────────┬──────────────────────┬───────────────────────┘
           ▼                      ▼
┌──────────────────┐   ┌──────────────────────┐
│   Remote Source   │   │    Local Source       │
│  Retrofit (API)   │   │  Room DB + DataStore  │
│  (Spring Boot     │   │  (캐시, 설정값,        │
│   서버와 통신)     │   │   오프라인 데이터)      │
└──────────────────┘   └──────────────────────┘
```

### 왜 MVVM인가?
- **Google 공식 권장** — Android Developer 문서 기본 패턴
- **Compose와 찰떡** — StateFlow로 상태 관리하면 Compose가 자동으로 UI 업데이트
- **자료 풍부** — 막혔을 때 검색하면 해결책이 바로 나옴
- **팀원과 협업 쉬움** — 화면(UI) ↔ 로직(ViewModel) ↔ 데이터(Repository) 역할 분리가 명확

---

## 2. 프로젝트 패키지 구조

```
com.naeda.app/
│
├── 📁 di/                          ← 의존성 주입 (Hilt 모듈)
│   ├── NetworkModule.kt            ← Retrofit, OkHttp 설정
│   ├── DatabaseModule.kt           ← Room DB 설정
│   └── RepositoryModule.kt         ← Repository 바인딩
│
├── 📁 data/                        ← 데이터 계층
│   ├── 📁 remote/                  ← 서버 통신
│   │   ├── 📁 api/                 ← Retrofit API 인터페이스
│   │   │   ├── AuthApi.kt          ← 로그인/회원가입
│   │   │   ├── BankingApi.kt       ← 계좌/이체/거래내역
│   │   │   ├── PaymentApi.kt       ← 결제 관련
│   │   │   ├── PointApi.kt         ← 포인트 적립/사용/후원
│   │   │   └── AiApi.kt            ← 소비분석/맛집추천/FDS
│   │   └── 📁 dto/                 ← 서버 응답 데이터 클래스
│   │       ├── AccountDto.kt
│   │       ├── TransactionDto.kt
│   │       ├── PaymentDto.kt
│   │       └── PointDto.kt
│   │
│   ├── 📁 local/                   ← 로컬 저장소
│   │   ├── 📁 db/                  ← Room Database
│   │   │   ├── NaedaDatabase.kt
│   │   │   ├── 📁 dao/
│   │   │   │   ├── AccountDao.kt
│   │   │   │   └── TransactionDao.kt
│   │   │   └── 📁 entity/
│   │   │       ├── AccountEntity.kt
│   │   │       └── TransactionEntity.kt
│   │   └── DataStoreManager.kt     ← 설정값, 토큰 저장
│   │
│   ├── 📁 repository/              ← Repository 구현체
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── BankingRepositoryImpl.kt
│   │   ├── PaymentRepositoryImpl.kt
│   │   ├── PointRepositoryImpl.kt
│   │   └── AiRepositoryImpl.kt
│   │
│   └── 📁 model/                   ← 앱 내부 모델 (UI에서 사용)
│       ├── Account.kt
│       ├── Transaction.kt
│       ├── Payment.kt
│       ├── Point.kt
│       └── Restaurant.kt
│
├── 📁 domain/                      ← 도메인 계층 (인터페이스)
│   └── 📁 repository/
│       ├── AuthRepository.kt
│       ├── BankingRepository.kt
│       ├── PaymentRepository.kt
│       ├── PointRepository.kt
│       └── AiRepository.kt
│
├── 📁 ui/                          ← UI 계층
│   ├── 📁 theme/                   ← 앱 테마, 색상, 타이포
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt
│   │
│   ├── 📁 navigation/              ← 네비게이션 설정
│   │   ├── NavGraph.kt             ← 전체 네비게이션 그래프
│   │   ├── Screen.kt               ← 화면 Route 정의
│   │   └── BottomNavBar.kt         ← 하단 탭 바
│   │
│   ├── 📁 auth/                    ← 인증 화면
│   │   ├── LoginScreen.kt
│   │   ├── LoginViewModel.kt
│   │   ├── SignUpScreen.kt
│   │   └── SignUpViewModel.kt
│   │
│   ├── 📁 home/                    ← 홈 화면
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   │
│   ├── 📁 banking/                 ← 뱅킹 화면
│   │   ├── AccountScreen.kt        ← 계좌 조회
│   │   ├── AccountViewModel.kt
│   │   ├── TransferScreen.kt       ← 이체
│   │   ├── TransferViewModel.kt
│   │   ├── TransactionScreen.kt    ← 거래내역
│   │   └── TransactionViewModel.kt
│   │
│   ├── 📁 facepay/                 ← 페이스페이
│   │   ├── FaceRegisterScreen.kt   ← 얼굴 등록
│   │   ├── FaceRegisterViewModel.kt
│   │   └── 📁 camera/
│   │       └── CameraPreview.kt    ← CameraX 프리뷰 컴포넌트
│   │
│   ├── 📁 point/                   ← 포인트/후원
│   │   ├── PointScreen.kt          ← 포인트 현황
│   │   ├── PointViewModel.kt
│   │   ├── DonationScreen.kt       ← 후원 화면
│   │   └── DonationViewModel.kt
│   │
│   ├── 📁 report/                  ← 소비 리포트
│   │   ├── ReportScreen.kt
│   │   └── ReportViewModel.kt
│   │
│   ├── 📁 recommend/               ← 맛집 추천
│   │   ├── RecommendScreen.kt
│   │   └── RecommendViewModel.kt
│   │
│   └── 📁 common/                  ← 공통 UI 컴포넌트
│       ├── LoadingIndicator.kt
│       ├── ErrorDialog.kt
│       ├── NaedaButton.kt
│       └── NaedaCard.kt
│
├── 📁 ble/                         ← BLE 비콘 관련
│   ├── BleManager.kt               ← BLE 스캔/연결 관리
│   └── BleService.kt               ← 백그라운드 BLE 서비스
│
├── 📁 fcm/                         ← 푸시 알림
│   └── NaedaFirebaseService.kt     ← FCM 메시지 수신
│
├── 📁 util/                        ← 유틸리티
│   ├── Constants.kt                ← 상수 정의
│   ├── Extensions.kt               ← 확장 함수
│   └── Resource.kt                 ← API 응답 래퍼
│
├── NaedaApplication.kt             ← Application 클래스 (Hilt)
└── MainActivity.kt                 ← 엔트리 포인트
```

---

## 3. 핵심 라이브러리

| 카테고리 | 라이브러리 | 용도 |
|---------|-----------|------|
| UI | Jetpack Compose + Material3 | 화면 구성 |
| 네비게이션 | Navigation Compose | 화면 전환 |
| 네트워크 | Retrofit2 + OkHttp + Gson | Spring Boot 서버 통신 |
| 로컬 DB | Room | 캐시, 오프라인 데이터 |
| 설정 저장 | DataStore | JWT 토큰, 사용자 설정 |
| DI | Hilt | 의존성 주입 |
| 상태 관리 | StateFlow + Compose State | UI 상태 관리 |
| 카메라 | CameraX | 얼굴 등록 촬영 |
| BLE | Android BLE API | 비콘 수신 |
| 푸시 | Firebase FCM | 결제 알림, FDS 경고 |
| 이미지 | Coil | 이미지 로딩 (맛집 사진 등) |
| 차트 | Vico (또는 MPAndroidChart) | 소비 리포트 차트 |

---

## 4. 데이터 흐름 예시: 계좌 조회

```
[AccountScreen]                      ← Compose 화면
    │ "화면 열림" 이벤트
    ▼
[AccountViewModel]                   ← 로직 처리
    │ accountRepository.getAccounts()
    ▼
[BankingRepositoryImpl]              ← 데이터 출처 결정
    │ ① 로컬 캐시 확인 (Room)
    │ ② 캐시 없거나 오래됨 → API 호출
    ▼
[BankingApi]                         ← Retrofit
    │ GET /api/accounts
    ▼
[Spring Boot 서버]                   ← SSAFY API 연동
    │ 응답: AccountDto
    ▼
[BankingRepositoryImpl]
    │ ① DTO → Model 변환
    │ ② Room에 캐시 저장
    │ ③ Model 반환
    ▼
[AccountViewModel]
    │ _uiState.value = AccountUiState.Success(accounts)
    ▼
[AccountScreen]
    │ uiState를 관찰 → 자동으로 화면 업데이트!
    └── 계좌 목록 표시
```

---

## 5. API 통신 구조

### 5.1 공통 응답 래퍼
```kotlin
// 모든 API 호출 결과를 감싸는 클래스
sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
    class Loading<T> : Resource<T>()
}
```

### 5.2 JWT 토큰 관리
```
로그인 성공 → AccessToken + RefreshToken 저장 (DataStore)
    ↓
모든 API 요청 → OkHttp Interceptor가 자동으로 헤더에 토큰 추가
    ↓
401 Unauthorized → Authenticator가 RefreshToken으로 자동 갱신
    ↓
갱신 실패 → 로그인 화면으로 이동
```

---

## 6. 네비게이션 구조

```
📱 앱 실행
│
├── 🔐 인증 플로우 (로그인 전)
│   ├── 스플래시 → 토큰 확인
│   ├── 로그인
│   ├── 회원가입
│   └── 1원 인증
│
└── 🏠 메인 플로우 (로그인 후) — 하단 탭 네비게이션
    │
    ├── 🏠 홈 탭
    │   ├── 잔액 요약
    │   ├── 최근 거래
    │   ├── 포인트 현황
    │   └── 퀵 액션 (이체, 얼굴 등록)
    │
    ├── 💰 뱅킹 탭
    │   ├── 계좌 목록
    │   ├── 계좌 상세 → 거래내역
    │   ├── 이체
    │   └── 적금 상품
    │
    ├── 😀 페이스페이 탭
    │   ├── 얼굴 등록 (미등록 시)
    │   ├── 등록 완료 상태
    │   ├── 결제 내역
    │   └── BLE 설정
    │
    ├── 📊 리포트 탭
    │   ├── 월간 소비 리포트
    │   ├── 카테고리별 차트
    │   ├── 지역 기여 점수
    │   └── 절약 인사이트
    │
    └── ⭐ 더보기 탭
        ├── 포인트 현황/사용
        ├── 할인권 교환
        ├── 후원하기
        ├── 맛집 추천
        ├── 설정
        └── 알림 내역
```

---

## 7. 화면별 UiState 패턴

모든 화면은 동일한 패턴으로 상태를 관리:

```kotlin
// 예: 계좌 화면 상태
data class AccountUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val error: String? = null
)

// ViewModel에서
class AccountViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()
    
    fun loadAccounts() {
        _uiState.update { it.copy(isLoading = true) }
        // API 호출 → 성공/실패에 따라 상태 업데이트
    }
}

// Screen에서
@Composable
fun AccountScreen(viewModel: AccountViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorDialog(uiState.error)
        else -> AccountList(uiState.accounts)
    }
}
```

---

## 8. 파트별 작업 분리 가이드

사용자 앱 2명이 동시에 작업할 때 충돌을 최소화하는 구조:

| 파트 A (페이스페이 + BLE) | 파트 B (뱅킹 + 서비스) |
|--------------------------|----------------------|
| `ui/facepay/` | `ui/banking/` |
| `ui/home/` (퀵 액션) | `ui/home/` (잔액, 거래) |
| `ble/` | `ui/point/` |
| `data/remote/api/PaymentApi.kt` | `ui/report/` |
| `data/repository/PaymentRepositoryImpl.kt` | `ui/recommend/` |
| `ui/common/` (카메라 관련) | `data/remote/api/BankingApi.kt` |
| | `data/remote/api/PointApi.kt` |
| | `data/remote/api/AiApi.kt` |

**공통 작업 (1주차에 같이 세팅)**:
- `di/` 모듈 세팅
- `ui/theme/` 디자인 시스템
- `ui/navigation/` 네비게이션 그래프
- `data/model/` 공통 모델
- `util/` 유틸리티

---

## 9. build.gradle 핵심 의존성

```kotlin
// build.gradle.kts (app)
dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ViewModel + Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit (네트워크)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room (로컬 DB)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // CameraX (얼굴 등록)
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Coil (이미지)
    implementation("io.coil-kt:coil-compose:2.7.0")
}
```

---

## 10. 개발 우선순위 (사용자 앱)

| 순서 | 작업 | 이유 |
|------|------|------|
| 1 | 프로젝트 세팅 + 테마 + 네비게이션 | 모든 화면의 기반 |
| 2 | 로그인/회원가입 + JWT 관리 | API 호출의 전제 조건 |
| 3 | 홈 화면 + 계좌 조회 | 핵심 뱅킹 기능 |
| 4 | 얼굴 등록 (CameraX) | 프로젝트 핵심 (2주차 마일스톤) |
| 5 | BLE 비콘 수신 | 2차 인증 연동 |
| 6 | 이체 + 거래내역 | 뱅킹 완성 |
| 7 | 포인트 + 후원 | ESG 핵심 |
| 8 | 소비 리포트 + 맛집 추천 | AI 기능 연동 |
