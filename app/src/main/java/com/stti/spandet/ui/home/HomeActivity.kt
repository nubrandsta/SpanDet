package com.stti.spandet.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.stti.spandet.data.Repository
import com.stti.spandet.data.preferences.UserPreferences
import com.stti.spandet.databinding.ActivityHomeBinding
import com.stti.spandet.ui.collection.ProcessActivity
import com.stti.spandet.tools.convertMillisToDirName
import com.stti.spandet.ui.main.CollectionViewActivity
import com.stti.spandet.ui.main.adapters.collectionListAdapter
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: collectionListAdapter
    private lateinit var repository: Repository

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var locationName: String = "Somewhere"
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    private lateinit var prefs: UserPreferences

    private var username:String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndRequestPermissions()

        repository = Repository(this)

        prefs = UserPreferences(this)

        username = prefs.getUsername().toString()

        binding.tvName.text = "Selamat Datang, ${username}"

        adapter = collectionListAdapter { collection ->
            // intent to collection view activity
            val selectedCollection = collection.name
            val intent = Intent(this, CollectionViewActivity::class.java)
            intent.putExtra("collection_name", selectedCollection)
            startActivity(intent)
        }

        binding.rvCollections.layoutManager = LinearLayoutManager(this)
        binding.rvCollections.adapter = adapter

        lifecycleScope.launch {
            val collections = repository.scanCollectionsDir(username)
            if (collections.isEmpty()) {
                binding.rvCollections.visibility = View.GONE
                binding.emptyPrompt.visibility = View.VISIBLE
            } else {
                binding.rvCollections.visibility = View.VISIBLE
                binding.emptyPrompt.visibility = View.GONE
                adapter.submitList(collections)
            }
        }

        binding.btnStart.setOnClickListener {
            val dirname = convertMillisToDirName(System.currentTimeMillis())

            lifecycleScope.launch {
                val existingCollections = repository.scanCollectionsDir(username).map { it.name }
                if (dirname in existingCollections) {
                    AlertDialog.Builder(this@HomeActivity)
                        .setMessage("Sebuah koleksi dengan nama ini sudah ada!.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {

                    val timenow = System.currentTimeMillis()
                    repository.createCollectionDir(dirname, timenow, locationName, lat, lon, username)
                    Toast.makeText(this@HomeActivity, "Koleksi berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@HomeActivity, ProcessActivity::class.java)
                    intent.putExtra("collection_name", dirname)
                    startActivity(intent)
                    finish()
                }
            }


        }

    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val collections = repository.scanCollectionsDir(username)
            if (collections.isEmpty()) {
                binding.rvCollections.visibility = View.GONE
                binding.emptyPrompt.visibility = View.VISIBLE
            } else {
                binding.rvCollections.visibility = View.VISIBLE
                binding.emptyPrompt.visibility = View.GONE
                adapter.submitList(collections)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val deniedPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(deniedPermissions.toTypedArray())
        } else {
            getCurrentLocation()
        }
    }

    // 🔹 Permission Request Handler
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Izin diperlukan untuk menggunakan fitur ini!", Toast.LENGTH_LONG).show()
                checkAndRequestPermissions()
            }
        }


    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                    reverseGeocode(lat, lon)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔹 Reverse Geocode Location to Address
    private fun reverseGeocode(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Build the custom address string you want
                locationName = buildString {
                    if (!address.subLocality.isNullOrEmpty()) append(" ${address.subLocality}")
                    if (!address.locality.isNullOrEmpty()) append(", ${address.locality}")
                    if (!address.adminArea.isNullOrEmpty()) append(", ${address.subAdminArea}")
                }

                binding.tvLocation.text = locationName
            } else {
                binding.tvLocation.text = "Unknown Location"
            }
        } catch (e: Exception) {
            binding.tvLocation.text = "Failed to get location"
            Toast.makeText(this, "Gagal mendapatkan alamat lengkap", Toast.LENGTH_SHORT).show()
        }
    }
}