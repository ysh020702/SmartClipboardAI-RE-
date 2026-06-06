package com.samsung.smartclipboard.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSourceExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val ocrRecognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )

    suspend fun extractFromOcr(uriString: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val uri = Uri.parse(uriString)
            val image = loadAndCropBitmap(uri)
            val result = Tasks.await(ocrRecognizer.process(image))
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun loadAndCropBitmap(uri: Uri, cropVerticalPercent: Float = 0.1f): InputImage {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val cropTop = (original.height * cropVerticalPercent).toInt()
        val cropBottom = (original.height * cropVerticalPercent).toInt()
        val cropHeight = original.height - cropTop - cropBottom

        val croppedBitmap = if (cropHeight <= 0) {
            original
        } else {
            Bitmap.createBitmap(original, 0, cropTop, original.width, cropHeight)
        }

        return InputImage.fromBitmap(croppedBitmap, 0)
    }

    suspend fun extractFromUrl(url: String): String {
        val correctionUrl = url.replace("://blog.naver.com","://m.blog.naver.com")
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(correctionUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0"
            )
            .build()

        val html = withContext(Dispatchers.IO) {
            client.newCall(request)
                .execute()
                .use {
                    it.body?.string() ?: ""
                }
        }

        val article = Readability4J(
            correctionUrl,
            html
        ).parse()

        return article.textContent.toString()
    }
}
