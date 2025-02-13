package com.stti.spandet.ui.main

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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.stti.spandet.R
import com.stti.spandet.data.Repository
import com.stti.spandet.data.model.BoundingBox
import com.stti.spandet.data.model.ProcessImage
import com.stti.spandet.databinding.ActivityProcessBinding
import com.stti.spandet.detector.Constants.DEFECT_DETECTOR_PATH
import com.stti.spandet.detector.Constants.DEFECT_LABELS_PATH
import com.stti.spandet.detector.Constants.WORKPIECE_DETECTOR_PATH
import com.stti.spandet.detector.Constants.WORKPIECE_LABELS_PATH
import com.stti.spandet.detector.Detector
import com.stti.spandet.tools.getImageUri
import com.stti.spandet.ui.main.adapters.imageListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream

class ProcessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProcessBinding
    private lateinit var adapter: imageListAdapter
    private lateinit var repository: Repository

    private lateinit var workpiece_detector: Detector
    private lateinit var defect_detector: Detector

    private var images: MutableList<ProcessImage> = mutableListOf()

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

        // Register ActivityResult handler
        val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // Handle permission requests results
            // See the permission example in the Android platform samples: https://github.com/android/platform-samples
        }

        // Permission request logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
        } else {
            requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }



        val collectionName = intent.getStringExtra("collection_name") ?: return

        repository = Repository(this)

        // Set up detectors
        workpiece_detector = Detector(baseContext, WORKPIECE_DETECTOR_PATH, WORKPIECE_LABELS_PATH, object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                Log.d("Detector", "No workpiece detected.")
            }

            override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                Log.d("Detector", "Workpiece detected with inference time: $inferenceTime ms")
            }
        })

        defect_detector = Detector(baseContext, DEFECT_DETECTOR_PATH, DEFECT_LABELS_PATH, object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                Log.d("Detector", "No defects detected.")
            }

            override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                Log.d("Detector", "Defects detected with inference time: $inferenceTime ms")
            }
        })

        workpiece_detector.setup()
        defect_detector.setup()

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
            processAllImages(collectionName)
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

    private fun processAllImages(collectionName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val totalImages = images.size
            binding.progressIndicator.max = totalImages
            binding.progressIndicator.progress = 0
            binding.progressIndicator.visibility = View.VISIBLE

            images.forEachIndexed { index, processImage ->
                withContext(Dispatchers.IO) {
                    val timeNow = System.currentTimeMillis()
                    val originalImageFile = saveOriginalImage(processImage.uri, collectionName, timeNow)
                    val workpieceResult = processWorkpieceDetection(processImage.uri, collectionName, timeNow)
                    if (workpieceResult != null) {
                        Log.d("ProcessImage", "Workpiece detected, proceeding to defect detection.")
                        processDefectDetection(processImage.uri, workpieceResult.first, workpieceResult.second)
                    } else {
                        Log.e("ProcessImage", "No workpiece detected, saving the plain image for URI: ${processImage.uri}")
                        savePlainImage(processImage.uri, collectionName)
                    }
                }
                binding.progressIndicator.progress = index + 1
            }

            binding.progressIndicator.visibility = View.GONE

            Intent(this@ProcessActivity, CollectionViewActivity::class.java).apply {
                putExtra("collection_name", collectionName)
                startActivity(this)
            }

            finish()
        }
    }

    private fun processWorkpieceDetection(uri: Uri, collectionName: String, timeNow: Long): Pair<File, File>? {
        Log.d("ProcessImage", "Starting workpiece detection for URI: $uri")

        val bitmap = uriToBitmap(uri) ?: return null
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, workpiece_detector.tensorWidth, workpiece_detector.tensorHeight, false)

        val tensorImage = workpiece_detector.imageProcessor.process(TensorImage.fromBitmap(resizedBitmap))
        val output = TensorBuffer.createFixedSize(intArrayOf(1, workpiece_detector.numChannel, workpiece_detector.numElements), DataType.FLOAT32)

        try {
            workpiece_detector.interpreter?.run(tensorImage.buffer, output.buffer.rewind())
        } catch (e: Exception) {
            Log.e("ProcessImage", "Error during workpiece detection inference: ${e.message}")
            return null
        }

        val bestBoxes = workpiece_detector.bestBox(output.floatArray)
        if (bestBoxes.isNullOrEmpty()) {
            Log.d("ProcessImage", "No workpiece detected for URI: $uri")
            return null
        }
        val fileName = "${collectionName}_${timeNow}"
        val resultDir = getResultDirectory(collectionName)
        val jsonFile = File(resultDir, "$fileName.json")
        val imageFile = File(resultDir, "$fileName.jpg")

        return Pair(jsonFile, imageFile)
    }

    private fun processDefectDetection(uri: Uri, jsonFile: File, imageFile: File) {
        Log.d("ProcessImage", "Starting defect detection for URI: $uri")

        val bitmap = uriToBitmap(uri) ?: return

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, defect_detector.tensorWidth, defect_detector.tensorHeight, false)
        val tensorImage = defect_detector.imageProcessor.process(TensorImage.fromBitmap(resizedBitmap))
        val output = TensorBuffer.createFixedSize(intArrayOf(1, defect_detector.tensorHeight, defect_detector.tensorWidth, defect_detector.numChannel), DataType.FLOAT32)

        try {
            defect_detector.interpreter?.run(tensorImage.buffer, output.buffer.rewind())
        } catch (e: Exception) {
            Log.e("ProcessImage", "Error during defect detection inference: ${e.message}")
            return
        }

        val bestBoxes = defect_detector.bestBox(output.floatArray)
        if (bestBoxes.isNullOrEmpty()) {
            Log.d("ProcessImage", "No defects detected for URI: $uri")
            return
        }

        try {
            // Save defect detection JSON (overwrite workpiece JSON)
            val json = boundingBoxesToJson(bestBoxes)
            jsonFile.writeText(json.toString())
            Log.d("FileSave", "Defect JSON saved to ${jsonFile.absolutePath}")

            // Draw bounding boxes on the image and save it (overwrite workpiece image)
            val resultBitmap = drawBoundingBoxesOnBitmap(bitmap, bestBoxes)
            saveBitmapToFile(resultBitmap, imageFile)
            Log.d("FileSave", "Defect image with bounding boxes saved to ${imageFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("ProcessImage", "Failed to save defect detection files: ${e.message}")
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
