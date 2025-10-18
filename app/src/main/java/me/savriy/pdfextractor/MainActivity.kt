package me.savriy.pdfextractor

import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            extractPdfFromAssetsToJpg("book.pdf")
        }
    }

    private suspend fun extractPdfFromAssetsToJpg(
        assetPdfName: String,
        scale: Float = 6f,
    ) {
        withContext(Dispatchers.IO) {
            val tempPdf = File(cacheDir, assetPdfName)
            assets.open(assetPdfName).use { input ->
                tempPdf.outputStream().use { output -> input.copyTo(output) }
            }

            // Open PDF using PdfRenderer
            val fileDescriptor =
                ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)

            // Render each page
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)

                // Create high-resolution bitmap
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = createBitmap(width, height)

                // Apply matrix scale for sharp rendering
                val matrix = Matrix().apply { postScale(scale, scale) }

                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                ImageSaver.saveImageToGallery(
                    context = this@MainActivity,
                    bitmap = bitmap,
                )

                bitmap.recycle()

                "Page $i extracted.".also {
                    Log.d("LOG_TAG", it)
                }
            }

            renderer.close()
            fileDescriptor.close()

            "PDF pages have been extracted and saved as JPG images.".also {
                Log.d("LOG_TAG", it)
            }
        }
    }
}