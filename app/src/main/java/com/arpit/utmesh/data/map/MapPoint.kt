package com.arpit.utmesh.data.map

data class MapPoint(
    val lat: Double,
    val lng: Double,
    val label: String,
    val details: String? = null
)
