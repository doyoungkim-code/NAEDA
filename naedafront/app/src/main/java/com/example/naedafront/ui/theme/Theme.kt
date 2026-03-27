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
    primaryContainer = Mint100,      // #44E3D3 연한 민트 (선택된 상태 배경)
    onPrimaryContainer = Mint900,    // #00635A 딥 그린

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

// 다크 테마 — #0D1A1A 기반 틸 계열
private val NaedaDarkColorScheme = darkColorScheme(
    primary = Mint400,               // #00E3CC 밝은 민트 (다크에서 눈에 잘 띄게)
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF0F3D35),
    onPrimaryContainer = Mint100,

    secondary = Blue400,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF162D3D),
    onSecondaryContainer = Blue300,

    tertiary = Sky400,
    onTertiary = DarkBackground,

    error = Color(0xFFFF6B6B),
    onError = DarkBackground,
    errorContainer = Color(0xFF3D1616),
    onErrorContainer = Color(0xFFFF6B6B),

    background = DarkBackground,          // #0D1A1A
    onBackground = DarkOnBackground,      // #E2ECEC
    surface = DarkSurface,                // #152626
    onSurface = DarkOnSurface,            // #DAE6E6
    surfaceVariant = DarkSurfaceVariant,  // #1E3232
    onSurfaceVariant = DarkOnSurfaceVariant, // #87A0A0

    outline = DarkOutline,                // #2D4A4A
    outlineVariant = DarkOutlineVariant,  // #1F3636

    inverseSurface = Color(0xFFDAE6E6),
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