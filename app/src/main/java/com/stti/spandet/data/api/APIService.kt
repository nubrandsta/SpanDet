package com.stti.spandet.data.api

import com.stti.spandet.data.api.response.ResponseChange
import com.stti.spandet.data.api.response.ResponseLogin
import com.stti.spandet.data.api.response.ResponseUpload
import com.stti.spandet.data.api.response.ResponseValidate
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Field
import retrofit2.http.Part

interface ApiService {

    @FormUrlEncoded
    @POST("/api/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): ResponseLogin

    @FormUrlEncoded
    @POST("/api/validate-token")
    suspend fun validateToken(
        @Field("token") token: String
    ): ResponseValidate

    @FormUrlEncoded
    @POST("/api/change-pass")
    suspend fun changePassword(
        @Field("username") username: String,
        @Field("old_password") old_pass: String,
        @Field("new_password") new_pass: String
    ): ResponseChange

    @Multipart
    @POST("/api/upload")
    suspend fun uploadImage(
        @Part("session") session: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lon") lon: RequestBody,
        @Part("thoroughfare") thoroughfare: RequestBody,
        @Part("subloc") subloc: RequestBody,
        @Part("locality") locality: RequestBody,
        @Part("subadmin") subadmin: RequestBody,
        @Part("adminArea") admin: RequestBody,
        @Part("postalcode") postalcode: RequestBody,
        @Part("spandukCount") spandukCount: RequestBody,
        @Part("image") image: MultipartBody.Part
    ) : ResponseUpload

}