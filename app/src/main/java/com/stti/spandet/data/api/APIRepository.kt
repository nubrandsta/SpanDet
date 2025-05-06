package com.stti.spandet.data.api

import android.content.Context
import androidx.lifecycle.liveData
import com.google.gson.Gson
import com.stti.spandet.data.api.response.ResponseChange
import com.stti.spandet.data.api.response.ResponseLogin
import com.stti.spandet.data.api.response.ResponseUpload
import com.stti.spandet.data.api.response.ResponseValidate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import retrofit2.HttpException

class APIRepository private constructor(
    private val ApiServices: ApiService
) {


    fun login(username: String, password:String) = liveData {
        emit(ResultState.Loading)
        try {
            val response = ApiServices.login(username, password)
            emit(ResultState.Success(response))
        }
        catch (e: HttpException){
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, ResponseLogin::class.java)
            emit(errorResponse.message?.let { ResultState.Error(it) })
        }
    }

    fun validate(session: String) = liveData {
        emit(ResultState.Loading)
        try {
            val response = ApiServices.validateToken(session)
            emit(ResultState.Success(response))
        }
        catch (e: HttpException){
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, ResponseValidate::class.java)
            emit(errorResponse.message?.let { ResultState.Error(it) })
        }
    }

    fun changePassword(username: String, old_pass: String, new_pass: String) = liveData {
        emit(ResultState.Loading)
        try {
            val response = ApiServices.changePassword(username, old_pass, new_pass)
            emit(ResultState.Success(response))
        }
        catch ( e: HttpException){
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, ResponseChange::class.java)
            emit(errorResponse.message?.let { ResultState.Error(it) })
        }

    }

    fun uploadImage(session: String, lat: Double, lon: Double, thoroughfare: String, subloc: String, locality: String, subadmin: String, adminArea: String, postalcode: String,spandukCount: Int, image: File) = liveData {
        emit(ResultState.Loading)
        val requestBody_Session = session.toRequestBody("text/plain".toMediaType())
        val requestBody_Lat = lat.toString().toRequestBody("text/plain".toMediaType())
        val requestBody_Lon = lon.toString().toRequestBody("text/plain".toMediaType())
        val requestBody_Thoroughfare = thoroughfare.toRequestBody("text/plain".toMediaType())
        val requestBody_Subloc = subloc.toRequestBody("text/plain".toMediaType())
        val requestBody_Locality = locality.toRequestBody("text/plain".toMediaType())
        val requestBody_Subadmin = subadmin.toRequestBody("text/plain".toMediaType())
        val requestBody_Adminarea = adminArea.toRequestBody("text/plain".toMediaType())
        val requestBody_Postalcode = postalcode.toRequestBody("text/plain".toMediaType())
        val requestBody_SpandukCount = spandukCount.toString().toRequestBody("text/plain".toMediaType())
        val requestFile_Image = image.asRequestBody("image/jpeg".toMediaType())

        val multipartBody = MultipartBody.Part.createFormData(
            "image",
            image.name,
            requestFile_Image
        )

        try{
            val response = ApiServices.uploadImage(requestBody_Session, requestBody_Lat, requestBody_Lon, requestBody_Thoroughfare, requestBody_Subloc, requestBody_Locality, requestBody_Subadmin, requestBody_Adminarea, requestBody_Postalcode, requestBody_SpandukCount, multipartBody)
            emit(ResultState.Success(response))
        }

        catch(e: HttpException){
            val errorBody  =e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, ResponseUpload::class.java)
            emit(errorResponse.message?.let { ResultState.Error(it) })
        }
    }
    companion object {
        @Volatile
        private var instance: APIRepository? = null
        fun getInstance(apiService: ApiService) =
            instance ?: synchronized(this) {
                instance ?: APIRepository(apiService)
            }.also { instance = it }
    }
}