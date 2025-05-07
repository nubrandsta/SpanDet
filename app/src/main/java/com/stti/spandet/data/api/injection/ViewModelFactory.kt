package com.stti.spandet.data.api.injection

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stti.spandet.data.api.APIRepository
import com.stti.spandet.ui.auth.ChangePasswordViewModel
import com.stti.spandet.ui.auth.LoginViewModel
import com.stti.spandet.ui.collection.UploadImageViewModel
import com.stti.spandet.ui.init.SplashViewModel

class ViewModelFactory private constructor(private val repository: APIRepository) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        when {

            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ChangePasswordViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return ChangePasswordViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return SplashViewModel(repository) as T
            }
            modelClass.isAssignableFrom(UploadImageViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return UploadImageViewModel(repository) as T
            }




            else -> throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
        }
    }

    companion object {
        @Volatile
        private var instance: ViewModelFactory? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ViewModelFactory(Injection.provideRepository())
            }.also { instance = it }
    }

}