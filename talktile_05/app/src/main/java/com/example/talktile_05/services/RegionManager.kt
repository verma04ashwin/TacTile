package com.example.talktile_05.services

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads polygon regions from an asset JSON.
 * Expected JSON array format (each entry):
 * {
 *   "image": "map_image.jpg",
 *   "category": "Some Region",
 *   "segmentation": [ [x1,y1,x2,y2,...] ]
 * }
 */
class RegionManager(private val context: Context) {

    private data class PolygonEntry(val image: String?, val category: String, val segmentation: List<Pair<Float,Float>>)

    private var polygons: List<PolygonEntry> = emptyList()
    private var imageFileName: String? = null

    fun loadFromAsset(assetPath: String) {
        try {
            context.assets.open(assetPath).use { stream ->
                val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
                parseJson(text)
            }
            Log.d("RegionManager", "Loaded ${polygons.size} polygons from $assetPath")
        } catch (e: Exception) {
            Log.e("RegionManager", "Failed to load asset $assetPath: ${e.message}")
            polygons = emptyList()
            imageFileName = null
        }
    }

    private fun parseJson(raw: String) {
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PolygonEntry>()
            var imgName: String? = null

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val image = if (obj.has("image")) obj.getString("image") else null
                if (imgName == null) imgName = image

                val cat = obj.optString("category", "Unknown")
                val segRoot = obj.opt("segmentation")
                val segPoints = mutableListOf<Pair<Float,Float>>()

                // segmentation can be nested (array of arrays)
                if (segRoot is JSONArray) {
                    val first = segRoot.optJSONArray(0)
                    if (first != null) {
                        var idx = 0
                        while (idx < first.length()) {
                            val x = first.optDouble(idx).toFloat(); val y = first.optDouble(idx+1).toFloat()
                            segPoints.add(Pair(x, y))
                            idx += 2
                        }
                    }
                }

                if (segPoints.isNotEmpty()) {
                    list.add(PolygonEntry(image, cat, segPoints))
                }
            }

            polygons = list
            imageFileName = imgName
        } catch (e: Exception) {
            Log.e("RegionManager", "JSON parse error: ${e.message}")
            polygons = emptyList()
            imageFileName = null
        }
    }

    fun getImageFileName(): String? = imageFileName

    fun getTouchedRegion(x: Float, y: Float): String? {
        for (entry in polygons) {
            if (pointInPolygon(x, y, entry.segmentation)) return entry.category
        }
        return null
    }

    // Ray-casting even-odd rule
    private fun pointInPolygon(px: Float, py: Float, polygon: List<Pair<Float,Float>>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].first
            val yi = polygon[i].second
            val xj = polygon[j].first
            val yj = polygon[j].second

            val intersect = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi + 1e-6f) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
