package com.example.tactile_main

data class Region(
    val image: String,
    val category: String,
    val segmentation: List<List<Float>>
)
