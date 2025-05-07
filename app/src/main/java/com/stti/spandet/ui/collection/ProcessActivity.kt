package com.stti.spandet.ui.collection

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.stti.spandet.R
import com.stti.spandet.data.Repository
import com.stti.spandet.data.model.BoundingBox
import com.stti.spandet.data.model.ProcessImage
import com.stti.spandet.databinding.ActivityProcessBinding
import com.stti.spandet.detector.Constants.SPANDUK_DETECTOR_PATH
import com.stti.spandet.detector.Constants.SPANDUK_LABELS_PATH
import com.stti.spandet.detector.Detector
import com.stti.spandet.tools.getImageUri
import com.stti.spandet.ui.main.CollectionViewActivity
import com.stti.spandet.ui.main.adapters.imageListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import com.stti.spandet.data.api.injection.ViewModelFactory
import com.stti.spandet.data.preferences.UserPreferences


class ProcessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProcessBinding
    private lateinit var adapter: imageListAdapter
    private lateinit var repository: Repository

    private lateinit var factory: ViewModelFactory
    private val viewModel by viewModels<UploadImageViewModel> {
        ViewModelFactory.getInstance()
    }

    private lateinit var spanduk_detector: Detector

    private lateinit var dirname: String

    private var images: MutableList<ProcessImage> = mutableListOf()

    private var currentImgCount: Int = 0
    private var currentDetections: Int = 0

    private var session: String = ""

    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        factory = ViewModelFactory.getInstance()
        val preferences = UserPreferences(this)
        session = preferences.getToken().toString()

        val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->

        }

        // Permission request logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
        } else {
            requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }

        repository = Repository(this)
        val prefs = UserPreferences(this)

        dirname = intent.getStringExtra("collection_name") ?: return

        lifecycleScope.launch {
            val collection = repository.getCollectionMetadata(dirname)
            if (collection != null) {
                Log.d("Process", "Collection Metadata: $collection")

            } else {
                Toast.makeText(this@ProcessActivity, "Collection not found", Toast.LENGTH_SHORT).show()
            }
        }

        repository = Repository(this)



        spanduk_detector = Detector(baseContext, SPANDUK_DETECTOR_PATH, SPANDUK_LABELS_PATH, object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                Log.d("Detector", "No adverts detected.")
            }

            override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                Log.d("Detector", "Adverts detected with inference time: $inferenceTime ms")
            }
        })

//        workpiece_detector.setup()
//        defect_detector.setup()

        spanduk_detector.setup()

        adapter = imageListAdapter { processImage ->
            // Handle click on processImage if needed
        }

        binding.rvCollection.adapter = adapter
        binding.rvCollection.layoutManager = GridLayoutManager(this, 2)

        CoroutineScope(Dispatchers.Main).launch {
//            images = repository.scanImages(collectionName).toMutableList()

            adapter.submitList(images)

            if (images.isEmpty()) {
                binding.rvCollection.visibility = View.GONE
                binding.emptyPrompt.visibility = View.VISIBLE
                binding.uploadButton.isEnabled = false
            } else {
                binding.rvCollection.visibility = View.VISIBLE
                binding.emptyPrompt.visibility = View.GONE
                binding.uploadButton.isEnabled = true
            }
        }

        setupImageCapture()

        binding.uploadButton.setOnClickListener {
            processAllImages(dirname)
        }
    }

    private fun setupImageCapture() {
        val launcherGallery = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                Log.d("Photo Picker", "Media selected, uri: $uri")
                addImageToAdapter(uri)
            } else {
                Log.d("Photo Picker", "No media selected")
            }
        }

        val launcherIntentCamera = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { isSuccess ->
            if (isSuccess) {
                currentImageUri?.let {
                    addImageToAdapter(it)
                }
            } else {
                currentImageUri = null
            }
        }

        val requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    currentImageUri = getImageUri(this)
                    launcherIntentCamera.launch(currentImageUri!!)
                } else {
                    Log.d("Camera Permission", "Camera permission denied")
                }
            }

        binding.galleryButton.setOnClickListener {
            launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.cameraButton.setOnClickListener {
            when {
                checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                    currentImageUri = getImageUri(this)
                    launcherIntentCamera.launch(currentImageUri!!)
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                    Log.d("Camera Permission", "Camera permission is needed to take pictures")
                }
                else -> {
                    requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            }
        }
    }

    private fun addImageToAdapter(uri: Uri) {
        val newImage = ProcessImage(uri)
        images.add(newImage)
        adapter.submitList(images.toList())
        adapter.notifyDataSetChanged()

        if (images.isEmpty()) {
            binding.rvCollection.visibility = View.GONE
            binding.emptyPrompt.visibility = View.VISIBLE
            binding.uploadButton.isEnabled = false
        } else {
            binding.rvCollection.visibility = View.VISIBLE
            binding.emptyPrompt.visibility = View.GONE
            binding.uploadButton.isEnabled = true
        }
    }

    // Dialog components for progress tracking
    private var processingDialog: androidx.appcompat.app.AlertDialog? = null
    private var dialogProgressBar: android.widget.ProgressBar? = null
    private var dialogProgressText: android.widget.TextView? = null
    private var dialogCurrentImageText: android.widget.TextView? = null
    
    private fun processAllImages(collectionName: String) {
        // Create and show a processing dialog
        processingDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Deteksi Spanduk")
            .setCancelable(false)
            .setView(layoutInflater.inflate(R.layout.dialog_processing, null))
            .create()
        
        processingDialog?.show()
        
        // Get dialog views
        dialogProgressBar = processingDialog?.findViewById(R.id.dialog_progress_bar)
        dialogProgressText = processingDialog?.findViewById(R.id.dialog_progress_text)
        dialogCurrentImageText = processingDialog?.findViewById(R.id.dialog_current_image_text)
        
        CoroutineScope(Dispatchers.Main).launch {
            val totalImages = images.size
            
            // Set up the progress bar
            dialogProgressBar?.max = totalImages
            dialogProgressBar?.progress = 0
            dialogProgressText?.text = "Memproses gambar 0 dari $totalImages"
            
            images.forEachIndexed { index, processImage ->
                // Update dialog for current image
                val currentImageNumber = index + 1
                dialogProgressText?.text = "Memproses gambar $currentImageNumber dari $totalImages"
                dialogCurrentImageText?.text = "Menyimpan gambar..."
                
                withContext(Dispatchers.IO) {
                    val timeNow = System.currentTimeMillis()
                    
                    // Update UI from background thread
                    withContext(Dispatchers.Main) {
                        dialogCurrentImageText?.text = "Menyimpan gambar..."
                    }
                    saveOriginalImage(processImage.uri, collectionName, timeNow)
                    
                    // Update UI for detection phase
                    withContext(Dispatchers.Main) {
                        dialogCurrentImageText?.text = "Melakukan deteksi..."
                    }
                    processSpandukDetection(processImage.uri, collectionName, timeNow)
                }
                
                // Update progress
                dialogProgressBar?.progress = currentImageNumber
            }
            
            // Show completion message briefly before dismissing
            dialogProgressText?.text = "Pemrosesan Selesai!"
            dialogCurrentImageText?.text = "Memproses $totalImages gambar"
            
            // Delay slightly to show completion message
            delay(1000)
            
            // Dismiss dialog when complete
            processingDialog?.dismiss()
            
            // Navigate to collection view
            Intent(this@ProcessActivity, CollectionViewActivity::class.java).apply {
                putExtra("collection_name", collectionName)
                startActivity(this)
            }
            
            finish()
        }
    }


    private fun processSpandukDetection(uri: Uri, collectionName: String, timeNow: Long) {
        Log.d("ProcessImage", "Memulai Deteksi Gambar: $uri")

        // Update UI with current status
        CoroutineScope(Dispatchers.Main).launch {
            dialogCurrentImageText?.text = "memuat Gambar..."
        }
        
        val bitmap = uriToBitmap(uri) ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            dialogCurrentImageText?.text = "Menyiapkan Gambar..."
        }
        
        val resizedBitmap = bitmap.scale(spanduk_detector.tensorWidth, spanduk_detector.tensorHeight, false)

        val tensorImage = spanduk_detector.imageProcessor.process(TensorImage.fromBitmap(resizedBitmap))
        val output = TensorBuffer.createFixedSize(intArrayOf(1, spanduk_detector.numChannel, spanduk_detector.numElements), DataType.FLOAT32)

        CoroutineScope(Dispatchers.Main).launch {
            dialogCurrentImageText?.text = "Melakukan deteksi..."
        }
        
        try {
            spanduk_detector.interpreter?.run(tensorImage.buffer, output.buffer.rewind())
        } catch (e: Exception) {
            Log.e("ProcessImage", "Terjadi Kesalahan Deteksi!: ${e.message}")
            CoroutineScope(Dispatchers.Main).launch {
                dialogCurrentImageText?.text = "Error during detection: ${e.message}"
            }
            return
        }

        val detectedBoxes = spanduk_detector.bestBox(output.floatArray)
        val numDetections = detectedBoxes?.size ?: 0
        
        CoroutineScope(Dispatchers.Main).launch {
            if (!detectedBoxes.isNullOrEmpty()) {
                dialogCurrentImageText?.text = "Deteksi $numDetections spanduk. Menyimpan Hasil..."
            } else {
                dialogCurrentImageText?.text = "Spanduk Tidak Ditemukan, Menyimpan Hasil..."
            }
        }

        val fileName = "${collectionName}_${timeNow}"
        val resultDir = getResultDirectory(collectionName)
        val jsonFile = File(resultDir, "$fileName.json")
        val imageFile = File(resultDir, "$fileName.jpg")

        try {
            if (!detectedBoxes.isNullOrEmpty()) {
                // Save JSON only if spanduk is detected
                val json = boundingBoxesToJson(detectedBoxes)
                jsonFile.writeText(json.toString())
                Log.d("FileSave", "Spanduk JSON saved to ${jsonFile.absolutePath}")

                // Draw bounding boxes and save
                val resultBitmap = drawBoundingBoxesOnBitmap(bitmap, detectedBoxes)
                saveBitmapToFile(resultBitmap, imageFile)
                Log.d("FileSave", "Spanduk image with bounding boxes saved to ${imageFile.absolutePath}")
            } else {
                // No detection, save plain image only
                saveBitmapToFile(bitmap, imageFile)
                Log.d("FileSave", "No spanduk detected, plain image saved to ${imageFile.absolutePath}")
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val current = repository.getCollectionMetadata(collectionName)
                val newImgCount = (current?.imgCount ?: 0) + 1
                val newDetections = (current?.detections ?: 0) + numDetections
                repository.updateCollectionMetadata(collectionName, newImgCount, newDetections)
            }
            
            CoroutineScope(Dispatchers.Main).launch {
                dialogCurrentImageText?.text = "Image processed successfully"
            }
            
        } catch (e: Exception) {
            Log.e("ProcessImage", "Failed to save spanduk detection files: ${e.message}")
            CoroutineScope(Dispatchers.Main).launch {
                dialogCurrentImageText?.text = "Error saving results: ${e.message}"
            }
        }
    }




    private fun saveOriginalImage(uri: Uri, collectionName: String, timeNow: Long): File? {
        val bitmap = uriToBitmap(uri) ?: return null
        val fileName = "${collectionName}_original_${timeNow}.jpg"
        val imageDir = getImageDirectory(collectionName)
        val imageFile = File(imageDir, fileName)

        saveBitmapToFile(bitmap, imageFile)
        Log.d("FileSave", "Original image saved to ${imageFile.absolutePath}")
        return imageFile
    }

    private fun savePlainImage(uri: Uri, collectionName: String) {
        val bitmap = uriToBitmap(uri) ?: return

        val fileName = "${collectionName}_${System.currentTimeMillis()}.jpg"
        val resultDir = getResultDirectory(collectionName)
        val imageFile = File(resultDir, fileName)

        saveBitmapToFile(bitmap, imageFile)
        Log.d("FileSave", "Plain image saved to ${imageFile.absolutePath}")
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            Log.e("UriToBitmap", "Failed to decode bitmap from URI: $uri, Error: ${e.message}")
            null
        }
    }

    private fun drawBoundingBoxesOnBitmap(bitmap: Bitmap, boundingBoxes: List<BoundingBox>): Bitmap {
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        boundingBoxes.forEach { box ->
            // Get the color for the class
            val classColor = getColorForClass(box.clsName)

            // Set up paint for the outline
            val outlinePaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = classColor
            }

            // Set up paint for the fill with 30% opacity
            val fillPaint = Paint().apply {
                style = Paint.Style.FILL
                color = classColor
                alpha = 77  // 30% opacity
            }

            // Set up paint for the text
            val detectTextSize = (resultBitmap.width * 0.02).coerceAtLeast(20.0)  // Dynamic text size
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = detectTextSize.toFloat()
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            // Calculate the bounding box coordinates
            val left = box.x1 * resultBitmap.width
            val top = box.y1 * resultBitmap.height
            val right = box.x2 * resultBitmap.width
            val bottom = box.y2 * resultBitmap.height

            // Draw the filled rectangle
            canvas.drawRect(left, top, right, bottom, fillPaint)

            // Draw the outline
            canvas.drawRect(left, top, right, bottom, outlinePaint)

            // Prepare the label text
            val label = box.clsName

            // Draw the text on the left outside of the bounding box
            val textX = right - textPaint.measureText(label) - 10  // Left side of the bounding box with padding
            val textY = top + textPaint.textSize  // Align the text with the top of the bounding box

            // Draw the text
            canvas.drawText(label, textX, textY, textPaint)
        }
        return resultBitmap
    }

    private fun getColorForClass(className: String): Int {
        return when (className) {
            "adj" -> ContextCompat.getColor(this@ProcessActivity, R.color.adjacent)
            "int" -> ContextCompat.getColor(this@ProcessActivity, R.color.integrity)
            "geo" -> ContextCompat.getColor(this@ProcessActivity, R.color.geometry)
            "pro" -> ContextCompat.getColor(this@ProcessActivity, R.color.postproc)
            "non" -> ContextCompat.getColor(this@ProcessActivity, R.color.nonpen)
            else -> ContextCompat.getColor(this@ProcessActivity, R.color.danger)
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }

    private fun boundingBoxesToJson(boundingBoxes: List<BoundingBox>): JSONObject {
        val jsonArray = org.json.JSONArray()
        boundingBoxes.forEach { box ->
            val jsonObject = JSONObject().apply {
                put("x1", box.x1)
                put("y1", box.y1)
                put("x2", box.x2)
                put("y2", box.y2)
                put("confidence", box.cnf)
                put("class", box.cls)
                put("class_name", box.clsName)
            }
            jsonArray.put(jsonObject)
        }
        return JSONObject().put("detections", jsonArray)
    }

    private fun getResultDirectory(collectionName: String): File {
        val resultDir = File(File(filesDir, "collections"), "$collectionName/result")
        if (!resultDir.exists()) {
            resultDir.mkdirs()
            Log.d("Directory", "Created result directory: ${resultDir.absolutePath}")
        }
        return resultDir
    }

    private fun getImageDirectory(collectionName: String): File {
        val imageDir = File(File(filesDir, "collections"), "$collectionName/image")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
            Log.d("Directory", "Created image directory: ${imageDir.absolutePath}")
        }
        return imageDir
    }
}
