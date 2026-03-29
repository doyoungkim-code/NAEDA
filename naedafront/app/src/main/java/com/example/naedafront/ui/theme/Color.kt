package com.example.naedafront.ui.theme

import androidx.compose.ui.graphics.Color

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 내다(NAEDA) Color System
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// ── Primary (민트/틸 계열) ──
val Mint50 = Color(0xFFE0F7F4)       // 가장 연한 민트 (배경 틴트용)
val Mint100 = Color(0xFF44E3D3)      // 연한 민트
val Mint200 = Color(0xFF00E3CC)      // 밝은 민트
val Mint400 = Color(0xFF32A89C)      // 중간 민트
val Mint500 = Color(0xFF009688)      // 틸 그린 (Primary)
val Mint700 = Color(0xFF00796B)      // 진한 틸
val Mint900 = Color(0xFF00635A)      // 딥 그린 (Primary Dark)

// ── Secondary (블루 계열) ──
val Navy900 = Color(0xFF012340)      // 네이비 다크
val Blue600 = Color(0xFF307CBF)      // 미디엄 블루
val Blue400 = Color(0xFF369AD9)      // 액센트 블루
val Blue300 = Color(0xFF8BB4D9)      // 라이트 블루
val Sky400 = Color(0xFF16B4F2)       // 스카이 블루

// ── Semantic (의미별 색상) ──
val Success = Color(0xFF038C3E)      // 결제 완료, 성공
val Warning = Color(0xFFF2CB05)      // FDS 경고, 주의
val Error = Color(0xFFF2522E)        // 에러, 결제 차단
val Info = Color(0xFF369AD9)         // 정보성 알림

// ── Neutral (중립 색상) ──
val Background = Color(0xFFFCFFFF)   // 전체 배경
val Surface = Color(0xFFFFFFFF)      // 카드, 시트 배경
val SurfaceVariant = Color(0xFFF2F2F2) // 보조 표면 (구분선 영역)

val OnBackground = Color(0xFF1A1A1A) // 배경 위 텍스트 (거의 블랙)
val OnSurface = Color(0xFF1A1A1A)    // 카드 위 텍스트
val OnSurfaceVariant = Color(0xFF6B6B6B) // 보조 텍스트 (힌트, 부제)
val OnPrimary = Color(0xFFFFFFFF)    // Primary 버튼 위 텍스트 (화이트)
val OnSecondary = Color(0xFFFFFFFF)  // Secondary 버튼 위 텍스트

val Outline = Color(0xFFD9D9D9)      // 테두리, 구분선
val OutlineVariant = Color(0xFFE8E8E8) // 연한 구분선

// ── Dark Theme (순수 검은색 계열) ──
val DarkBackground = Color(0xFF000000)      // 전체 배경 (순수 블랙)
val DarkSurface = Color(0xFF1C1C1E)         // 카드, 시트 배경
val DarkSurfaceVariant = Color(0xFF2C2C2E)  // 보조 표면 (입력 필드, 칩)
val DarkOnBackground = Color(0xFFE5E5E7)    // 배경 위 텍스트
val DarkOnSurface = Color(0xFFE5E5E7)       // 카드 위 텍스트
val DarkOnSurfaceVariant = Color(0xFF8E8E93) // 보조 텍스트 (힌트, 부제)
val DarkOutline = Color(0xFF38383A)         // 테두리, 구분선
val DarkOutlineVariant = Color(0xFF2C2C2E)  // 연한 구분선