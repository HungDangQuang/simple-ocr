/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.tensorflow.lite.examples.ocr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import android.view.Surface
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.examples.ocr.adapter.ImageAdapter
import org.tensorflow.lite.examples.ocr.databinding.TfeIsActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

//  private val tfImageName = "tensorflow.jpg"
//  private val androidImageName = "android.jpg"
//  private val chromeImageName = "chrome.jpg"
//  private lateinit var viewModel: MLExecutionViewModel
  private lateinit var resultImageView: ImageView
//  private lateinit var tfImageView: ImageView
//  private lateinit var androidImageView: ImageView
//  private lateinit var chromeImageView: ImageView
  private lateinit var chipsGroup: ChipGroup
  private lateinit var runButton: Button
//  private lateinit var textPromptTextView: TextView

  private var useGPU = false
  private var selectedImageName = "tensorflow.jpg"
  private var ocrModel: OCRModelExecutor? = null
//  private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
//  private val mainScope = MainScope()
  private val mutex = Mutex()
  private lateinit var viewBinding:TfeIsActivityMainBinding
  private lateinit var cameraExecutor: ExecutorService
  private val buttonClick = AlphaAnimation(1f, 0.8f)
  private var isReadyToOpenCamera = false
  private var imageCapture: ImageCapture? = null

  private val activityResultLauncher =
    registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions())
    { permissions ->
      // Handle Permission granted/rejected
      var permissionGranted = true
      permissions.entries.forEach {
        if (it.key in REQUIRED_PERMISSIONS && it.value == false)
          permissionGranted = false
      }
      if (!permissionGranted) {
        Toast.makeText(baseContext,
          "Permission request denied",
          Toast.LENGTH_SHORT).show()
      } else {
        isReadyToOpenCamera = true
        startCamera()
      }
    }

  private val broadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
      Log.d(TAG, "onReceive: ${p1!!.action}")
      if (p1.action.equals(ACTION_END_PROCESSING)) {
        viewBinding.progressBar.visibility = View.INVISIBLE
      } else if (p1.action.equals(ACTION_START_PROCESSING)) {
        viewBinding.progressBar.visibility = View.VISIBLE
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = TfeIsActivityMainBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)





//    val toolbar: Toolbar = findViewById(R.id.toolbar)
//    setSupportActionBar(toolbar)
//    supportActionBar?.setDisplayShowTitleEnabled(false)
//
//    tfImageView = findViewById(R.id.tf_imageview)
//    androidImageView = findViewById(R.id.android_imageview)
//    chromeImageView = findViewById(R.id.chrome_imageview)

//    val candidateImageViews = arrayOf<ImageView>(tfImageView, androidImageView, chromeImageView)

//    val assetManager = assets
//    try {
//      val tfInputStream: InputStream = assetManager.open(tfImageName)
//      val tfBitmap = BitmapFactory.decodeStream(tfInputStream)
//      tfImageView.setImageBitmap(tfBitmap)
//      val androidInputStream: InputStream = assetManager.open(androidImageName)
//      val androidBitmap = BitmapFactory.decodeStream(androidInputStream)
//      androidImageView.setImageBitmap(androidBitmap)
//      val chromeInputStream: InputStream = assetManager.open(chromeImageName)
//      val chromeBitmap = BitmapFactory.decodeStream(chromeInputStream)
//      chromeImageView.setImageBitmap(chromeBitmap)
//    } catch (e: IOException) {
//      Log.e(TAG, "Failed to open a test image")
//    }

//    for (iv in candidateImageViews) {
//      setInputImageViewListener(iv)
//    }

//    resultImageView = findViewById(R.id.result_imageview)
//    chipsGroup = findViewById(R.id.chips_group)
//    textPromptTextView = findViewById(R.id.text_prompt)
//    val useGpuSwitch: Switch = findViewById(R.id.switch_use_gpu)

//    viewModel = AndroidViewModelFactory(application).create(MLExecutionViewModel::class.java)
//    viewModel.resultingBitmap.observe(
//      this,
//      Observer { resultImage ->
//        if (resultImage != null) {
//          updateUIWithResults(resultImage)
//        }
//        enableControls(true)
//      }
//    )
//
//    mainScope.async(inferenceThread) { createModelExecutor(useGPU) }

//    useGpuSwitch.setOnCheckedChangeListener { _, isChecked ->
//      useGPU = isChecked
//      mainScope.async(inferenceThread) { createModelExecutor(useGPU) }
//    }

//    runButton = findViewById(R.id.rerun_button)
//    runButton.setOnClickListener {
//      enableControls(false)
//
//      mainScope.async(inferenceThread) {
//        mutex.withLock {
//          if (ocrModel != null) {
//            viewModel.onApplyModel(baseContext, selectedImageName, ocrModel, inferenceThread)
//          } else {
//            Log.d(
//              TAG,
//              "Skipping running OCR since the ocrModel has not been properly initialized ..."
//            )
//          }
//        }
//      }
//    }

//    setChipsToLogView(HashMap<String, Int>())
//    enableControls(true)

    initItem()

  }

  private suspend fun createModelExecutor(useGPU: Boolean) {
    mutex.withLock {
      if (ocrModel != null) {
        ocrModel!!.close()
        ocrModel = null
      }
      try {
        ocrModel = OCRModelExecutor(this, useGPU)
      } catch (e: Exception) {
        Log.e(TAG, "Fail to create OCRModelExecutor: ${e.message}")
        val logText: TextView = findViewById(R.id.log_view)
        logText.text = e.message
      }
    }
  }

  private fun setChipsToLogView(itemsFound: Map<String, Int>) {
    chipsGroup.removeAllViews()

    for ((word, color) in itemsFound) {
      val chip = Chip(this)
      chip.text = word
      chip.chipBackgroundColor = getColorStateListForChip(color)
      chip.isClickable = false
      chipsGroup.addView(chip)
    }
    val labelsFoundTextView: TextView = findViewById(R.id.tfe_is_labels_found)
    if (chipsGroup.childCount == 0) {
      labelsFoundTextView.text = getString(R.string.tfe_ocr_no_text_found)
    } else {
      labelsFoundTextView.text = getString(R.string.tfe_ocr_texts_found)
    }
    chipsGroup.parent.requestLayout()
  }

  private fun getColorStateListForChip(color: Int): ColorStateList {
    val states =
      arrayOf(
        intArrayOf(android.R.attr.state_enabled), // enabled
        intArrayOf(android.R.attr.state_pressed) // pressed
      )

    val colors = intArrayOf(color, color)
    return ColorStateList(states, colors)
  }

  private fun setImageView(imageView: ImageView, image: Bitmap) {
    Glide.with(baseContext).load(image).override(250, 250).fitCenter().into(imageView)
  }

  private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult) {
    setImageView(resultImageView, modelExecutionResult.bitmapResult)
    val logText: TextView = findViewById(R.id.log_view)
    logText.text = modelExecutionResult.executionLog

    setChipsToLogView(modelExecutionResult.itemsFound)
    enableControls(true)
  }

  private fun enableControls(enable: Boolean) {
    runButton.isEnabled = enable
  }

  private fun initItem() {
    Log.d(TAG, "initItem: ")
    viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
    val data = ArrayList<String>()
    data.add("android.jpg")
    data.add("chrome.jpg")
    data.add("tensorflow.jpg")
    val adapter = ImageAdapter(this, data)
    viewBinding.recyclerView.adapter = adapter

    viewBinding.openCameraBtn.setOnClickListener { view ->
      view.startAnimation(buttonClick)
      if (isReadyToOpenCamera) {
        takePhoto()
      } else {
        checkToOpenCamera()
      }
    }
    cameraExecutor = Executors.newSingleThreadExecutor()
    registerBroadcast()
  }

  private fun checkToOpenCamera() {
    Log.d(TAG, "checkToOpenCamera: ")
      if (allPermissionsGranted()) {
        Log.d(TAG, "checkToOpenCamera: all permissions are granted")
        isReadyToOpenCamera = true
        startCamera()
      } else {
        Log.d(TAG, "checkToOpenCamera: permisisons are not granted")
        requestPermission()
      }
  }

  private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    Log.d(TAG, "allPermissionsGranted: ")
    ContextCompat.checkSelfPermission(
      baseContext, it) == PackageManager.PERMISSION_GRANTED
  }

  private fun startCamera() {
    Log.d(TAG, "startCamera: ")
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
      // Used to bind the lifecycle of cameras to the lifecycle owner
      val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

      // Preview
      val preview = Preview.Builder()
        .build()
        .also {
          viewBinding.viewFinder.visibility = View.VISIBLE
          viewBinding.openCameraBtn.translationZ = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, getResources().getDisplayMetrics());
          it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
        }

      // Select back camera as a default
      val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

      try {
        // Unbind use cases before rebinding
        cameraProvider.unbindAll()

        imageCapture = ImageCapture.Builder().build()
        // Bind use cases to camera
        cameraProvider.bindToLifecycle(
          this, cameraSelector, preview, imageCapture)

      } catch(exc: Exception) {
        Log.e(TAG, "Use case binding failed", exc)
      }

    }, ContextCompat.getMainExecutor(this))
  }

  private fun requestPermission() {
    Log.d(TAG, "requestPermission: ")
    activityResultLauncher.launch(REQUIRED_PERMISSIONS)
  }

  private fun takePhoto() {
    Log.d(TAG, "takePhoto: ")

    // start loading
    val intent = Intent(ACTION_START_PROCESSING)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    // Get a stable reference of the modifiable image capture use case
    val imageCapture =  this.imageCapture ?: return


    // Set up image capture listener, which is triggered after photo has
    // been taken
    imageCapture.takePicture(ContextCompat.getMainExecutor(this),

    object: ImageCapture.OnImageCapturedCallback() {

      override fun onCaptureSuccess(image: ImageProxy) {

        Log.d(TAG, "onCaptureSuccess: rotation: ")
        val bitmap = ImageUtils.imageProxyToBitmap(image)

        saveToInternalStorage(bitmap!!)
//        ImageUtils.getOutputFile()

        super.onCaptureSuccess(image)
      }
    })
  }
  override fun onDestroy() {
    super.onDestroy()
    cameraExecutor.shutdown()
    LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
  }

   fun saveToInternalStorage(bitmapImage: Bitmap): String? {
    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OCR")

    if (!directory.exists()) {
      directory.mkdirs()
    }

    val fileName =
      "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"

    // Create imageDir
    val mypath = File(directory, fileName)

    var fos: FileOutputStream? = null
    try {
      fos = FileOutputStream(mypath)
      // Use the compress method on the BitMap object to write image to the OutputStream
      bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
    } catch (e: java.lang.Exception) {
      e.printStackTrace()
    } finally {
      try {
        fos!!.close()
        val intent = Intent(ACTION_END_PROCESSING)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }

    return mypath.absolutePath
  }

  private fun registerBroadcast() {
    Log.d(TAG, "registerBroadcast: ")

    val intentFilter = IntentFilter()

    intentFilter.addAction(ACTION_START_PROCESSING)
    intentFilter.addAction(ACTION_END_PROCESSING)

    LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)

  }
  companion object {
    private const val TAG = "OCR app"
    private val REQUIRED_PERMISSIONS =
      mutableListOf (
        android.Manifest.permission.CAMERA,
      ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
          add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
      }.toTypedArray()
    private const val ACTION_START_PROCESSING = "ACTION_START_PROCESSING"
    private const val ACTION_END_PROCESSING = "ACTION_END_PROCESSING"
  }
}
