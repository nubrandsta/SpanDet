package com.stti.spandet.data.api.response

import com.google.gson.annotations.SerializedName

data class ResponseUpload(

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("status")
    val status: String? = null
)