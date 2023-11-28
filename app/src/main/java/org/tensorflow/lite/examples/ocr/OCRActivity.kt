package org.tensorflow.lite.examples.ocr

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.examples.ocr.databinding.ActivityOcrBinding
import java.util.concurrent.Executors

class OCRActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityOcrBinding

    private var ocrModel: OCRModelExecutor? = null
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainScope = MainScope()
    private val mutex = Mutex()
    private lateinit var viewModel: MLExecutionViewModel
    private lateinit var fileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        fileName = intent.getStringExtra(STRING_EXTRA_FILENAME).toString()

        Log.d(TAG, "onCreate: $fileName")

        val bitmap = ImageUtils.loadImageFromName(fileName!!)

        viewBinding.ocrImage.setImageBitmap(bitmap)

        // Execute OCR

        viewModel = AndroidViewModelFactory(application).create(MLExecutionViewModel::class.java)
        viewModel.resultingBitmap.observe(
            this,
            Observer { resultImage ->
                if (resultImage != null) {
                    updateUIWithResults(resultImage)
                }
            }
        )


        mainScope.async(inferenceThread) {
            createModelExecutor(true)
            mutex.withLock {
                if (ocrModel != null) {
                    viewModel.onApplyModel(baseContext, fileName, ocrModel, inferenceThread)
                } else {
                    Log.d(
                        TAG,
                        "Skipping running OCR since the ocrModel has not been properly initialized ..."
                    )
                }
            }
        }
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
            }
        }
    }

    private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult) {
        viewBinding.ocrImage.setImageBitmap(modelExecutionResult.bitmapResult)
        viewBinding.ocrText.text = getTextResult(modelExecutionResult.itemsFound)
    }

    private fun getTextResult(itemsFound: Map<String, Int>) : String {
        var finalRes = ""
        for ((word, color) in itemsFound) {
            finalRes = "$word "
        }
        return finalRes
    }
    companion object {
        val STRING_EXTRA_FILENAME = "fileName"
        val TAG = "OCRActivity"
    }
}