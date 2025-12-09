package com.example.talktile_05.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * RegionManager — FINAL VERSION
 *
 * Supports OLD FORMAT ONLY:
 *
 * [
 *   {
 *     "category": "Punjab",
 *     "segmentation": [
 *       [x1, y1, x2, y2, x3, y3, ...]
 *     ]
 *   }
 * ]
 *
 * Maps YOLO → canonical map coordinates → polygon hit detection.
 */
class RegionManager(private val context: Context) {

    data class RegionPolygon(
        val category: String,
        val points: List<Pair<Float, Float>>
    )

    private var polygons: List<RegionPolygon> = emptyList()
    private var currentFile: String? = null

    /**
     * Load polygon JSON from assets.
     * Example path:
     *   "contemporary india/Chapter 2/soil_map.json"
     */
    fun loadRegionsFromAsset(assetPath: String) {
        Log.d("RegionManager", "Loading region polygons from: $assetPath")

        try {
            val input = context.assets.open(assetPath)
            val text = BufferedReader(InputStreamReader(input)).use { it.readText() }

            Log.d("RegionManager", "Raw region JSON loaded. Size = ${text.length} chars")

            polygons = parseJson(text)
            currentFile = assetPath

            Log.d("RegionManager", "Parsed ${polygons.size} polygons from $assetPath")

            polygons.forEach {
                Log.d("RegionManager", "Loaded region: ${it.category} with ${it.points.size} points")
            }

        } catch (e: Exception) {
            Log.e(
                "RegionManager",
                "FAILED to load region polygons from: $assetPath. Error=${e.message}"
            )
            e.printStackTrace()

            polygons = emptyList()
            currentFile = null
        }
    }


    fun getRegions(): List<RegionPolygon> = polygons
    fun getCurrentFileName(): String? = currentFile

    /**
     * Given canonical coordinates, return the region category name.
     */
    fun getTouchedRegion(x: Float, y: Float): String? {
        for (poly in polygons) {
            if (polygonContains(poly.points, x, y)) return poly.category
        }
        return null
    }

    // ------------------------------------------------------------
    // Parse JSON
    // ------------------------------------------------------------
    private fun parseJson(json: String): List<RegionPolygon> {
        val arr = JSONArray(json)
        val list = mutableListOf<RegionPolygon>()

        for (i in 0 until arr.length()) {
            val obj: JSONObject = arr.getJSONObject(i)
            val category = obj.getString("category")

            val segArray = obj.getJSONArray("segmentation").getJSONArray(0)

            val pts = mutableListOf<Pair<Float, Float>>()
            var j = 0
            while (j < segArray.length()) {
                val x = segArray.getDouble(j).toFloat()
                val y = segArray.getDouble(j + 1).toFloat()
                pts.add(x to y)
                j += 2
            }

            list.add(RegionPolygon(category, pts))
        }
        return list
    }

    // ------------------------------------------------------------
    // Point in Polygon — Ray casting
    // ------------------------------------------------------------
    private fun polygonContains(pts: List<Pair<Float, Float>>, px: Float, py: Float): Boolean {
        var inside = false
        var j = pts.size - 1

        for (i in pts.indices) {
            val (xi, yi) = pts[i]
            val (xj, yj) = pts[j]

            val intersect =
                ((yi > py) != (yj > py)) &&
                        (px < (xj - xi) * (py - yi) / ((yj - yi).takeIf { it != 0f } ?: 0.00001f) + xi)

            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
