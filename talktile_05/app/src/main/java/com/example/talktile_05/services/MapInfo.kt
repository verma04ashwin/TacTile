package com.example.talktile_05.services

/**
 * Represents a map present in a chapter page.
 */
data class MapInfo(
    val page: Int,
    val name: String,
    val description: String? = null
)
