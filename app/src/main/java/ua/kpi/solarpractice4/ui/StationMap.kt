package ua.kpi.solarpractice4.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ua.kpi.solarpractice4.model.Station

// osmdroid використовує OpenStreetMap; API-ключ Google не потрібний.
@Composable
fun StationMap(stations: List<Station>, selectedId: Int?, onSelect: (Int)->Unit,
               userPoint: GeoPoint? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val map = remember(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.4)
            controller.setCenter(GeoPoint(49.0, 31.0))
        }
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }
    AndroidView(factory = { map }, modifier=modifier,
        update = { view ->
            view.overlays.clear()
            stations.forEach { s ->
                val marker = Marker(view).apply {
                    position = GeoPoint(s.latitude,s.longitude)
                    title = s.name
                    snippet = "${s.type} · ${s.powerKw} кВт · ${if(s.online) "активна" else "неактивна"}"
                    setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { clicked, _ ->
                        onSelect(s.id)
                        clicked.showInfoWindow()
                        view.controller.animateTo(clicked.position)
                        true
                    }
                }
                view.overlays.add(marker)
                if (s.id == selectedId) {
                    view.controller.animateTo(marker.position)
                    view.controller.setZoom(8.0)
                    marker.showInfoWindow()
                }
            }
            userPoint?.let { point ->
                val userMarker = Marker(view).apply {
                    position = point
                    title = "Моє місцезнаходження"
                    snippet = "Отримано з геолокації пристрою"
                }
                view.overlays.add(userMarker)
            }
            view.invalidate()
        }
    )
}
