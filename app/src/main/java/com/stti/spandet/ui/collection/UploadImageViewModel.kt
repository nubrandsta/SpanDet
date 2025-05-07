package com.stti.spandet.ui.collection

import androidx.lifecycle.ViewModel
import com.stti.spandet.data.api.APIRepository
import java.io.File

class UploadImageViewModel(private val repository: APIRepository): ViewModel() {

    fun uploadImage(session: String, lat: Double, lon: Double, thoroughfare: String, subloc: String, locality: String, subadmin: String, adminArea: String, postalcode: String,spandukCount: Int, image: File) = repository.uploadImage(session, lat, lon, thoroughfare, subloc, locality, subadmin, adminArea, postalcode, spandukCount, image)

}