package com.example.naedafront.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 내다(NAEDA) Theme
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val NaedaLightColorScheme = lightColorScheme(
    // Primary — 앱의 메인 색상 (버튼, FAB, 강조)
    primary = Mint500,               // #009688 틸 그린
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

// 다크 테마 — 일단 기본 구조만 잡아둠 (나중에 필요하면 커스텀)
private val NaedaDarkColorScheme = darkColorScheme(
    primary = Mint200,               // 밝은 민트 (다크에서 눈에 잘 띄게)
    onPrimary = Mint900,
    primaryContainer = Mint700,
    onPrimaryContainer = Mint100,

    secondary = Blue400,
    onSecondary = Navy900,
    secondaryContainer = Navy900,
    onSecondaryContainer = Blue300,

    tertiary = Sky400,
    onTertiary = Navy900,

    error = Error,
    onError = OnPrimary,

    background = Navy900,
    onBackground = OnPrimary,
    surface = Navy900.copy(red = 0.05f, green = 0.15f, blue = 0.28f),
    onSurface = OnPrimary,
    surfaceVariant = Navy900.copy(red = 0.08f, green = 0.18f, blue = 0.32f),
    onSurfaceVariant = Blue300,

    outline = Blue600.copy(alpha = 0.5f),
    outlineVariant = Navy900.copy(alpha = 0.8f),
)

@Composable
fun NaedaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 현재는 라이트 모드만 사용 (금융 앱 특성상 라이트 우선)
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