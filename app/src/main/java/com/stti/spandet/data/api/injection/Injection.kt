package com.stti.spandet.data.api.injection

import android.content.Context
import com.stti.spandet.data.api.APIRepository
import com.stti.spandet.data.api.ApiConfig

object Injection {
    fun provideRepository(context: Context) : APIRepository {
        val apiService = ApiConfig.getApiService()
        return APIRepository(context, apiService)
    }
}