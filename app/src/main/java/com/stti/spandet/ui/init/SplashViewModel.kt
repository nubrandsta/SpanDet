package com.stti.spandet.ui.init

import androidx.lifecycle.ViewModel
import com.stti.spandet.data.Repository
import com.stti.spandet.data.api.APIRepository

class SplashViewModel(private val repository: APIRepository): ViewModel(){

    fun validate(token:String) = repository.validate(token)


}