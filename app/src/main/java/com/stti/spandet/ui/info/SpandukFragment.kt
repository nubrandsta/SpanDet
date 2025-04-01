package com.stti.spandet.ui.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stti.spandet.R
import com.stti.spandet.data.Repository
import com.stti.spandet.databinding.FragmentSpandukBinding
import com.stti.spandet.tools.convertIsoToReadable
import com.stti.spandet.tools.convertMillisToIsoTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpandukFragment : Fragment() {

    private lateinit var repository: Repository

    private var countSpanduk = 0
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var timestamp: Long? = null

    private var locationString = "Somewhere"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let{
            countSpanduk = it?.getInt("countSpanduk") ?: 0
            latitude = it?.getDouble("latitude")
            longitude = it?.getDouble("longitude")
            timestamp = it?.getLong("timestamp")
        }
        repository = Repository(requireContext())

        if(latitude != null && longitude != null){
            lifecycleScope.launch(Dispatchers.IO) {
                locationString = repository.reverseGeocodeLocation(latitude!!,longitude!!)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_spanduk, container, false)
        val tvSpandukCount: TextView = view.findViewById(R.id.tv_spanduk_count)
        val tvLocation: TextView = view.findViewById(R.id.tv_location_text)
        val tvTimestamp: TextView = view.findViewById(R.id.tv_time_text)

        tvSpandukCount.text = countSpanduk.toString()
        tvLocation.text = locationString
        tvTimestamp.text = convertIsoToReadable(convertMillisToIsoTime(timestamp!!))

        return view
    }

    companion object {

        @JvmStatic
        fun newInstance(countSpanduk: Int, latitude: Double, longitude: Double, timestamp: Long) =
            SpandukFragment().apply {
                arguments = Bundle().apply {
                    putInt("countSpanduk", countSpanduk)
                    putDouble("latitude", latitude)
                    putDouble("longitude", longitude)
                    putLong("timestamp", timestamp)
                }
            }
    }
}