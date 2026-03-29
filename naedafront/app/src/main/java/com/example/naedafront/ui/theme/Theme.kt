package com.example.naedafront.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 내다(NAEDA) Theme
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val NaedaLightColorScheme = lightColorScheme(
    // Primary — 앱의 메인 색상 (버튼, FAB, 강조)
    primary = Mint900,               // #00635A 딥 그린
    onPrimary = OnPrimary,           // 흰색 텍스트
    primaryContainer = Mint900,       // #00635A 딥 그린 (버튼 fill)
    onPrimaryContainer = OnPrimary,  // 흰색 (버튼 위 텍스트)

    // Secondary — 보조 색상 (칩, 필터, 토글)
    secondary = Blue600,             // #307CBF 미디엄 블루
    onSecondary = OnSecondary,       // 흰색 텍스트
    secondaryContainer = Blue300,    // #8BB4D9 라이트 블루
    onSecondaryContainer = Navy900,  // #012340 네이비

    // Tertiary — 세 번째 강조 (포인트 관련, 배지)
    tertiary = Sky400,               // #16B4F2 스카이 블루
    onTertiary = OnPrimary,
    tertiaryContainer = Mint50,      // #E0F7F4
    onTertiaryContainer = Mint900,

    // Error — 에러, 결제 차단
    error = Error,                   // #F2522E
    onError = OnPrimary,
    errorContainer = Error.copy(alpha = 0.12f),
    onErrorContainer = Error,

    // Background & Surface
    background = Background,         // #FCFFFF 전체 배경
    onBackground = OnBackground,     // #1A1A1A
    surface = Surface,               // #FFFFFF 카드 배경
    onSurface = OnSurface,           // #1A1A1A
    surfaceVariant = SurfaceVariant, // #F2F2F2
    onSurfaceVariant = OnSurfaceVariant, // #6B6B6B

    // Outline
    outline = Outline,               // #D9D9D9
    outlineVariant = OutlineVariant, // #E8E8E8
)

// 다크 테마 — 순수 블랙 계열
private val NaedaDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFCFFFF),      // #FCFFFF (텍스트/아이콘용)
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF1A3A35), // 어두운 초록 (버튼 fill용)
    onPrimaryContainer = Color(0xFFFCFFFF),

    secondary = Blue400,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF1A2A3A),
    onSecondaryContainer = Blue300,

    tertiary = Sky400,
    onTertiary = DarkBackground,

    error = Color(0xFFFF6B6B),
    onError = DarkBackground,
    errorContainer = Color(0xFF3A1A1A),
    onErrorContainer = Color(0xFFFF6B6B),

    background = DarkBackground,          // #000000
    onBackground = DarkOnBackground,      // #E5E5E7
    surface = DarkSurface,                // #1C1C1E
    onSurface = DarkOnSurface,            // #E5E5E7
    surfaceVariant = DarkSurfaceVariant,  // #2C2C2E
    onSurfaceVariant = DarkOnSurfaceVariant, // #8E8E93

    outline = DarkOutline,                // #38383A
    outlineVariant = DarkOutlineVariant,  // #2C2C2E

    inverseSurface = Color(0xFFE5E5E7),
    inverseOnSurface = DarkBackground,
    inversePrimary = Mint900,

    scrim = Color.Black,
)

@Composable
fun NaedaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) NaedaDarkColorScheme else NaedaLightColorScheme

    // 상태바 색상 설정
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NaedaTypography,
        content = content,
    )
}