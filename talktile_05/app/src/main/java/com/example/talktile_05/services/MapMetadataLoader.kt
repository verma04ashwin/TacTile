package com.example.talktile_05.services

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.BufferedReader

/**
 * Loads map metadata from:
 *
 * assets/<book>/<chapter>/maps.json
 *
 * Expected JSON format:
 *
 * [
 *   { "page": 12, "name": "Soil Types of India", "description": "Major soil regions" },
 *   { "page": 14, "name": "Annual Rainfall", "description": "Distribution map" }
 * ]
 */
class MapMetadataLoader(
    private val context: Context
) {

    fun loadMapInfo(book: String, chapter: String): List<MapInfo> {
        val path = "$book/$chapter/maps.json"

        Log.d("MapLoader", "Attempting to load map metadata from: $path")

        return try {
            val text = readAssetFile(path)
            if (text == null) {
                Log.e("MapLoader", "maps.json NOT FOUND at: $path")
                return emptyList()
            }

            Log.d("MapLoader", "maps.json loaded successfully: $path")
            val parsed = parseJson(text)

            Log.d("MapLoader", "Parsed mapInfoList = $parsed")

            parsed
        } catch (e: Exception) {
            Log.e("MapLoader", "Failed to load maps.json at $path", e)
            emptyList()
        }
    }


    // -----------------------------------------
    // JSON Parsing
    // -----------------------------------------
    private fun parseJson(raw: String): List<MapInfo> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<MapInfo>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)

                val page = obj.optInt("page", -1)
                val name = obj.optString("name", "")
                val desc = obj.optString("description", "")

                if (page >= 1 && name.isNotBlank()) {
                    list.add(MapInfo(page, name, desc))
                }
            }

            list
        } catch (e: Exception) {
            Log.e("MapMetadataLoader", "JSON parse error: ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------
    // Asset Reader
    // -----------------------------------------
    private fun readAssetFile(path: String): String? {
        return try {
            context.assets.open(path).use { stream ->
                BufferedReader(stream.reader()).readText()
            }
        } catch (_: Exception) {
            null
        }
    }
}
