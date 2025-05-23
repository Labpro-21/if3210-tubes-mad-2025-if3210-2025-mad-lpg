package com.tubes1.purritify.features.profile.presentation.profile.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapsActivity : ComponentActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        mapView = MapView(this)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        setContentView(mapView)

        mapView.controller.setZoom(5.0)
        mapView.controller.setCenter(GeoPoint(0.0, 0.0))

        val marker = Marker(mapView)
        mapView.overlays.add(marker)

        mapView.setOnTouchListener { v, event ->
            val projection = mapView.projection
            val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.invalidate()

            if (event.action == android.view.MotionEvent.ACTION_UP) {
                v.performClick()

                val resultIntent = Intent().apply {
                    putExtra("lat", geoPoint.latitude)
                    putExtra("lng", geoPoint.longitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}