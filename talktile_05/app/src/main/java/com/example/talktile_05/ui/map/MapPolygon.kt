package com.example.talktile_05.ui.map


data class MapPolygon(
    val category: String,
    val image: String,
    val points: List<Pair<Float, Float>>
) {

    // Ray-casting point-in-polygon
    fun contains(px: Float, py: Float): Boolean {
        var inside = false
        val n = points.size
        var j = n - 1

        for (i in 0 until n) {
            val xi = points[i].first
            val yi = points[i].second
            val xj = points[j].first
            val yj = points[j].second

            val intersect = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi + 0.00001f) + xi)

            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
