package com.stti.spandet.data.api

import com.stti.spandet.data.api.response.ResponseChange
import com.stti.spandet.data.api.response.ResponseLogin
import com.stti.spandet.data.api.response.ResponseUpload
import com.stti.spandet.data.api.response.ResponseValidate
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Field
import retrofit2.http.Part

interface ApiService {

    @FormUrlEncoded
    @POST("/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<ResponseLogin>

    @FormUrlEncoded
    @POST("/validate-token")
    suspend fun validateToken(
        @Field("token") token: String
    ): Response<ResponseValidate>

    @FormUrlEncoded
    @POST("/change-pass")
    suspend fun changePassword(
        @Field("username") username: String,
        @Field("old_password") old_pass: String,
        @Field("new_password") new_pass: String
    ): Response <ResponseChange>

    @Multipart
    @POST("/upload")
    suspend fun uploadImage(
        @Part("session") session: String,
        @Part("lat") lat: Double,
        @Part("lon") lon: Double,
        @Part("thoroughfare") thoroughfare: String,
        @Part("subloc") subloc: String,
        @Part("locality") locality: String,
        @Part("subadmin") subadmin: String,
        @Part("adminArea") admin: String,
        @Part("postalcode") postalcode: String,
        @Part("image") image: MultipartBody.Part
    ) : Response <ResponseUpload>

}