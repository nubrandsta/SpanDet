package com.stti.spandet.data.model

data class Collection (
    val name: String,
    val imgCount: Int,
    val detections: Int,
    val timestamp: Long,
    val locationString: String,
    val lat: Double,
    val lon: Double
    , val owner: String
)