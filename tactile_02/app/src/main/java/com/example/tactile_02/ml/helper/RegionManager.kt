package com.example.tactile_02.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * RegionManager:
 *  - Loads polygon data from JSON files in assets.
 *  - Keeps track of currently selected region map.
 */
class RegionManager(private val context: Context) {

    private var currentRegions: List<RegionPolygon> = emptyList()
    private var currentFile: String? = null

    data class RegionPolygon(
        val category: String,
        val points: List<Pair<Float, Float>>
    )

    /**
     * Load regions from a JSON file in assets.
     */
    fun loadRegionsFromAsset(fileName: String) {
        try {
            val inputStream = context.assets.open(fileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = bufferedReader.use { it.readText() }
            currentRegions = parseJson(jsonString)
            currentFile = fileName
            Log.d("RegionManager", "Loaded regions from $fileName with ${currentRegions.size} polygons")
        } catch (e: Exception) {
            Log.e("RegionManager", "Error loading regions: ${e.message}")
            currentRegions = emptyList()
        }
    }

    fun getTouchedRegion(x: Float, y: Float): String? {
        return currentRegions.firstOrNull { polygonContains(it.points, x, y) }?.category
    }

    // Simple point-in-polygon check
    private fun polygonContains(points: List<Pair<Float, Float>>, px: Float, py: Float): Boolean {
        var inside = false
        var j = points.size - 1

        Log.d("PolygonDebug", "Checking point ($px, $py) in polygon with ${points.size} vertices")

        for (i in points.indices) {
            val xi = points[i].first
            val yi = points[i].second
            val xj = points[j].first
            val yj = points[j].second

            // Log each edge being checked
            Log.d(
                "PolygonDebug",
                "Edge from ($xi,$yi) to ($xj,$yj)"
            )

            // Check if ray crosses the edge
            val intersect = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi + 0.000001f) + xi)

            if (intersect) {
                inside = !inside
                Log.d("PolygonDebug", "Ray intersected edge -> flip inside to $inside")
            }

            j = i
        }

        Log.d("PolygonDebug", "Final result for point ($px, $py): inside = $inside")
        return inside
    }

    /**
     * Returns current loaded regions.
     */
    fun getRegions(): List<RegionPolygon> {
        return currentRegions
    }

    /**
     * Returns the currently selected JSON file name.
     */
    fun getCurrentFileName(): String? {
        return currentFile
    }

    /**
     * Parse JSON string into RegionPolygon objects.
     */
    private fun parseJson(jsonString: String): List<RegionPolygon> {
        val regions = mutableListOf<RegionPolygon>()
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val obj: JSONObject = jsonArray.getJSONObject(i)
            val category = obj.getString("category")

            val segmentationArray = obj.getJSONArray("segmentation").getJSONArray(0)
            val points = mutableListOf<Pair<Float, Float>>()

            var j = 0
            while (j < segmentationArray.length()) {
                val x = segmentationArray.getDouble(j).toFloat()
                val y = segmentationArray.getDouble(j + 1).toFloat()
                points.add(Pair(x, y))
                j += 2
            }

            regions.add(RegionPolygon(category, points))
        }

        return regions
    }
}
