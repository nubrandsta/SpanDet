package com.stti.spandet.ui.init

import androidx.lifecycle.ViewModel
import com.stti.spandet.data.Repository

class SplashViewModel(private val repository: Repository): ViewModel(){


        fun checkCollectionDir(){
            repository.checkForCollectionsDir()
        }
}