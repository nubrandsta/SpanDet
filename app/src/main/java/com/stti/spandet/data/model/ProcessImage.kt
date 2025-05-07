package com.stti.spandet.data.model

import android.net.Uri

data class ProcessImage(
    val uri: Uri,
    val originalUri: Uri? = null,
    val fileName: String = "",
    val isEmpty: Boolean = true,
    val session: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val thoroughfare: String = "",
    val subLocality: String = "",
    val locality: String = "",
    val subAdminArea: String = "",
    val adminArea: String = "",
    val postalCode: String = "",
    val spandukCount: Int = 0
)