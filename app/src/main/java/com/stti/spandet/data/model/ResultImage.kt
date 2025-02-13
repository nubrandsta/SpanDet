package com.stti.spandet.data.model

import android.net.Uri

data class ResultImage(
    val fileName: String,
    val fileSize: Long,
    val uri: Uri,
    val originalUri: Uri = uri,
    val isEmpty: Boolean = false,
    val classOccurence: ClassOccurence = ClassOccurence()
)
