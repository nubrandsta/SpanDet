package com.stti.spandet.ui.auth

import androidx.lifecycle.ViewModel
import com.stti.spandet.data.api.APIRepository

class ChangePasswordViewModel(private val repository: APIRepository): ViewModel(){

    fun changePassword (username: String, old_pass: String, new_pass: String) = repository.changePassword(username, old_pass, new_pass)

}