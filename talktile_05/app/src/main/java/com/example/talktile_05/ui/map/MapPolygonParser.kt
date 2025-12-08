package com.example.talktile_05.ui.map

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object MapPolygonParser {

    fun load(context: Context, assetPath: String): List<MapPolygon> {
        val jsonStr = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val arr = JSONArray(jsonStr)

        val list = mutableListOf<MapPolygon>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            val image = obj.getString("image")
            val category = obj.getString("category")

            val segArray = obj.getJSONArray("segmentation").getJSONArray(0)

            val points = mutableListOf<Pair<Float, Float>>()
            var j = 0
            while (j < segArray.length()) {
                val x = segArray.getDouble(j).toFloat()
                val y = segArray.getDouble(j + 1).toFloat()
                points.add(x to y)
                j += 2
            }

            list.add(
                MapPolygon(
                    category = category,
                    image = image,
                    points = points
                )
            )
        }

        return list
    }
}
