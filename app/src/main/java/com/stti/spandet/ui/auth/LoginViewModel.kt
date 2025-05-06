package com.stti.spandet.ui.auth

import androidx.lifecycle.ViewModel
import com.stti.spandet.data.api.APIRepository

class LoginViewModel(private val repository: APIRepository): ViewModel(){

    fun login (username: String, password: String) = repository.login(username, password)
}