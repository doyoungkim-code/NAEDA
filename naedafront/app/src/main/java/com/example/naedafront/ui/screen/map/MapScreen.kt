package com.example.naedafront.ui.screen.map

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val mapPaddingPx = with(density) { 32.dp.roundToPx() }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())

            getMapAsync { naverMap ->
                setupMap(naverMap, mapPaddingPx)

                // TODO:
                // drawProvinceOverlays(naverMap)
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
    }
}

private fun setupMap(
    naverMap: NaverMap,
    paddingPx: Int,
) {
    naverMap.mapType = NaverMap.MapType.Navi
    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, true)
    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, false)
    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BICYCLE, false)
    naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRAFFIC, false)

    naverMap.uiSettings.isZoomControlEnabled = false
    naverMap.uiSettings.isScaleBarEnabled = false
    naverMap.uiSettings.isCompassEnabled = false
    naverMap.uiSettings.isIndoorLevelPickerEnabled = false
    naverMap.uiSettings.isLocationButtonEnabled = false

    naverMap.locationTrackingMode = LocationTrackingMode.NoFollow

    val koreaBounds = LatLngBounds(
        LatLng(33.0, 124.5),
        LatLng(38.9, 131.0)
    )

    naverMap.moveCamera(CameraUpdate.fitBounds(koreaBounds, paddingPx))
}