package com.example.naedafront.ui.screen.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.naedafront.data.remote.MapStoreResponseDto
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import kotlin.math.floor

private data class StoreMarkerCluster(
    val stores: List<MapStoreResponseDto>,
    val position: LatLng
)

@Composable
fun NaverRestaurantMapScreen(
    stores: List<MapStoreResponseDto>,
    hasLocationPermission: Boolean,
    currentLocationRequestKey: Int,
    onStoreClusterSelected: (List<MapStoreResponseDto>) -> Unit,
    onMapTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val mapPaddingPx = with(density) { 32.dp.roundToPx() }
    val activeMarkers = remember { mutableListOf<Marker>() }
    val latestStores by rememberUpdatedState(stores)
    val latestClusterSelected by rememberUpdatedState(onStoreClusterSelected)
    val latestOnMapTap by rememberUpdatedState(onMapTap)
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())

            getMapAsync { map ->
                setupMap(map, mapPaddingPx)
                naverMap = map
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

    DisposableEffect(naverMap, mapView) {
        val map = naverMap ?: return@DisposableEffect onDispose { }

        val cameraIdleListener = NaverMap.OnCameraIdleListener {
            renderStoreMarkers(
                naverMap = map,
                mapView = mapView,
                stores = latestStores,
                activeMarkers = activeMarkers,
                onClusterClick = latestClusterSelected
            )
        }

        map.addOnCameraIdleListener(cameraIdleListener)
        map.setOnMapClickListener { _, _ ->
            latestOnMapTap()
        }

        renderStoreMarkers(
            naverMap = map,
            mapView = mapView,
            stores = latestStores,
            activeMarkers = activeMarkers,
            onClusterClick = latestClusterSelected
        )

        onDispose {
            clearMarkers(activeMarkers)
            map.removeOnCameraIdleListener(cameraIdleListener)
        }
    }

    LaunchedEffect(stores, naverMap) {
        val map = naverMap ?: return@LaunchedEffect
        renderStoreMarkers(
            naverMap = map,
            mapView = mapView,
            stores = stores,
            activeMarkers = activeMarkers,
            onClusterClick = onStoreClusterSelected
        )
    }

    LaunchedEffect(hasLocationPermission, currentLocationRequestKey, naverMap) {
        val map = naverMap ?: return@LaunchedEffect
        if (hasLocationPermission) {
            moveCameraToCurrentLocation(context, map)
        } else {
            moveCameraToDefaultRegion(map, mapPaddingPx)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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

    moveCameraToDefaultRegion(naverMap, paddingPx)
}

private fun renderStoreMarkers(
    naverMap: NaverMap,
    mapView: MapView,
    stores: List<MapStoreResponseDto>,
    activeMarkers: MutableList<Marker>,
    onClusterClick: (List<MapStoreResponseDto>) -> Unit
) {
    clearMarkers(activeMarkers)

    if (stores.isEmpty() || mapView.width == 0 || mapView.height == 0) {
        return
    }

    val clusters = buildVisibleClusters(
        naverMap = naverMap,
        mapView = mapView,
        stores = stores
    )

    clusters.forEach { cluster ->
        val marker = Marker().apply {
            position = cluster.position
            icon = OverlayImage.fromBitmap(createClusterMarkerBitmap(cluster))
            width = Marker.SIZE_AUTO
            height = Marker.SIZE_AUTO
            isHideCollidedSymbols = true
            setOnClickListener {
                onClusterClick(
                    cluster.stores.sortedWith(
                        compareByDescending<MapStoreResponseDto> { it.rating }
                            .thenBy { it.storeName }
                    )
                )
                true
            }
            map = naverMap
        }
        activeMarkers += marker
    }
}

private fun buildVisibleClusters(
    naverMap: NaverMap,
    mapView: MapView,
    stores: List<MapStoreResponseDto>
): List<StoreMarkerCluster> {
    val zoom = naverMap.cameraPosition.zoom
    val cellSizePx = when {
        zoom >= 17.0 -> 46f
        zoom >= 15.0 -> 60f
        zoom >= 13.0 -> 78f
        zoom >= 11.0 -> 96f
        else -> 118f
    }

    val buckets = linkedMapOf<Pair<Int, Int>, MutableList<MapStoreResponseDto>>()
    val width = mapView.width.toFloat()
    val height = mapView.height.toFloat()

    stores.forEach { store ->
        val screenPoint = naverMap.projection.toScreenLocation(LatLng(store.latitude, store.longitude))
        if (screenPoint.x < -cellSizePx || screenPoint.x > width + cellSizePx) return@forEach
        if (screenPoint.y < -cellSizePx || screenPoint.y > height + cellSizePx) return@forEach

        val bucketKey = Pair(
            floor(screenPoint.x / cellSizePx).toInt(),
            floor(screenPoint.y / cellSizePx).toInt()
        )
        buckets.getOrPut(bucketKey) { mutableListOf() }.add(store)
    }

    return buckets.values.map { groupedStores ->
        val avgLatitude = groupedStores.map { it.latitude }.average()
        val avgLongitude = groupedStores.map { it.longitude }.average()
        StoreMarkerCluster(
            stores = groupedStores,
            position = LatLng(avgLatitude, avgLongitude)
        )
    }
}

private fun clearMarkers(activeMarkers: MutableList<Marker>) {
    activeMarkers.forEach { it.map = null }
    activeMarkers.clear()
}

private fun createClusterMarkerBitmap(cluster: StoreMarkerCluster): Bitmap {
    val isCluster = cluster.stores.size > 1
    val size = if (isCluster) 92 else 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isCluster) AndroidColor.parseColor("#152341") else AndroidColor.parseColor("#009688")
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.CENTER
        textSize = if (isCluster) 23f else 21f
        isFakeBoldText = true
    }
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#44E3D3")
        style = Paint.Style.FILL
    }
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0B1F32")
        textAlign = Paint.Align.CENTER
        textSize = 20f
        isFakeBoldText = true
    }

    val radius = if (isCluster) 28f else 22f
    val centerX = size * 0.46f
    val centerY = size * 0.54f
    canvas.drawCircle(centerX, centerY, radius, mainPaint)
    canvas.drawCircle(centerX, centerY, radius, strokePaint)
    canvas.drawText(if (isCluster) "식" else "점", centerX, centerY + 8f, textPaint)

    if (isCluster) {
        val badgeRadius = 16f
        val badgeX = size * 0.73f
        val badgeY = size * 0.28f
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)
        canvas.drawText(cluster.stores.size.toString(), badgeX, badgeY + 7f, badgeTextPaint)
    }

    return bitmap
}

private fun moveCameraToDefaultRegion(
    naverMap: NaverMap,
    paddingPx: Int
) {
    val gumiBounds = LatLngBounds(
        LatLng(36.00, 128.20),
        LatLng(36.23, 128.48)
    )

    naverMap.moveCamera(CameraUpdate.fitBounds(gumiBounds, paddingPx))
}

private fun moveCameraToCurrentLocation(
    context: Context,
    naverMap: NaverMap
) {
    if (!hasLocationPermission(context)) {
        return
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return

    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )

    val latestKnownLocation = providers
        .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }

    latestKnownLocation?.let { location ->
        updateMapToLocation(naverMap, location.latitude, location.longitude)
    }

    val currentProvider = providers.firstOrNull {
        runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false)
    } ?: return

    runCatching {
        locationManager.requestSingleUpdate(
            currentProvider,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    updateMapToLocation(naverMap, location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
            },
            Looper.getMainLooper()
        )
    }.onFailure {
        latestKnownLocation?.let {
            updateMapToLocation(naverMap, it.latitude, it.longitude)
        }
    }
}

private fun updateMapToLocation(
    naverMap: NaverMap,
    latitude: Double,
    longitude: Double
) {
    val currentLatLng = LatLng(latitude, longitude)
    naverMap.locationOverlay.position = currentLatLng
    naverMap.locationOverlay.isVisible = true
    naverMap.moveCamera(CameraUpdate.scrollAndZoomTo(currentLatLng, 15.0))
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
