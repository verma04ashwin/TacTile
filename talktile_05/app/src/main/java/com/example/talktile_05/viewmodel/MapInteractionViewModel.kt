package com.example.talktile_05.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.talktile_05.App
import com.example.talktile_05.ui.map.MapPolygon
import com.example.talktile_05.ui.map.MapPolygonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapInteractionViewModel : ViewModel() {

    private val _mapBitmap = MutableStateFlow<Bitmap?>(null)
    val mapBitmap = _mapBitmap.asStateFlow()

    private val _mapTitle = MutableStateFlow<String?>(null)
    val mapTitle = _mapTitle.asStateFlow()

    private val _polygons = MutableStateFlow<List<MapPolygon>>(emptyList())
    val polygons = _polygons.asStateFlow()

    private var canonicalWidth = 0f
    private var canonicalHeight = 0f

    fun loadMap(book: String, chapter: String, jsonFile: String) {
        viewModelScope.launch {
            val ctx = App.instance

            _mapTitle.value = jsonFile

            // 1. Load polygon JSON
            val folder = "contemporary india/$chapter"
            val jsonPath = "$folder/$jsonFile"
            val allPolygons = MapPolygonParser.load(ctx, jsonPath)

            _polygons.value = allPolygons

            // Determine canonical image ref
            canonicalWidth = 640f
            canonicalHeight = 640f

            // 2. Load the reference image used for polygons
            val firstImg = allPolygons.firstOrNull()?.image
            if (firstImg != null) {
                val imgPath = "$folder/$firstImg"
                _mapBitmap.value = loadBitmapFromAssets(ctx, imgPath)
            }
        }
    }

    private fun loadBitmapFromAssets(context: Context, assetPath: String): Bitmap? {
        return try {
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun onTap(tapX: Float, tapY: Float, displayWidth: Float, displayHeight: Float): String? {
        if (displayWidth == 0f || displayHeight == 0f) return null

        // Convert tap to canonical space
        val scaleX = canonicalWidth / displayWidth
        val scaleY = canonicalHeight / displayHeight

        val px = tapX * scaleX
        val py = tapY * scaleY

        // Find polygon hit
        for (poly in polygons.value) {
            if (poly.contains(px, py)) {
                return poly.category
            }
        }
        return null
    }
}
