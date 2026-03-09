package com.example.naedafront.ui.screen.map

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())

            getMapAsync { naverMap ->
                // 지도 느낌 바꾸기
                naverMap.mapType = NaverMap.MapType.Navi
                naverMap.isBuildingLayerGroupEnabled = true
                naverMap.isTransitLayerGroupEnabled = false
                naverMap.isBicycleLayerGroupEnabled = false
                naverMap.isTrafficLayerGroupEnabled = false

                // 기본 컨트롤 정리
                naverMap.uiSettings.isZoomControlEnabled = false
                naverMap.uiSettings.isScaleBarEnabled = false
                naverMap.uiSettings.isCompassEnabled = false
                naverMap.uiSettings.isIndoorLevelPickerEnabled = false
                naverMap.uiSettings.isLocationButtonEnabled = false

                // 카메라 테스트 위치
                val gumi = LatLng(36.1195, 128.3446)
                naverMap.cameraPosition = CameraPosition(gumi, 14.0)

                // 위치 추적 나중에 붙일 거면 여기서 활성화 가능
                naverMap.locationTrackingMode = LocationTrackingMode.NoFollow
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // 나중에 여기 위에 검색창, 필터칩, 하단 카드 올리면 앱 느낌 확 살아남
    }
}