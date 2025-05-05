package com.stti.spandet.data.api.response

import com.google.gson.annotations.SerializedName

data class ResponseValidate(

    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("message")
    val message: String? = null
)