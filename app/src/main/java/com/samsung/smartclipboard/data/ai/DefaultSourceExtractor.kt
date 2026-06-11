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
import com.samsung.smartclipboard.domain.model.LinkMetadata
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

    suspend fun extractFromUrl(url: String): LinkMetadata {
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

        val document = Jsoup.parse(html, correctionUrl)
        val article = Readability4J(
            correctionUrl,
            html
        ).parse()

        val description = document.metaContent("og:description")
            ?: document.metaContent("description")
            ?: document.metaContent("twitter:description")

        val textContent = article.textContent
            .toString()
            .trim()
            .takeIf { it.isNotBlank() && it != "null" }

        return LinkMetadata(
            title = document.metaContent("og:title")
                ?: document.metaContent("twitter:title")
                ?: document.title().takeIf { it.isNotBlank() },
            description = description,
            imageUrl = document.metaContent("og:image")?.toAbsoluteUrl(document)
                ?: document.metaContent("twitter:image")?.toAbsoluteUrl(document),
            textContent = listOfNotNull(description, textContent)
                .distinct()
                .joinToString("\n\n")
                .takeIf { it.isNotBlank() },
        )
    }

    private fun org.jsoup.nodes.Document.metaContent(key: String): String? {
        val propertyValue = selectFirst("meta[property=\"$key\"]")?.attr("content")
        val nameValue = selectFirst("meta[name=\"$key\"]")?.attr("content")
        return (propertyValue ?: nameValue)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun String.toAbsoluteUrl(document: org.jsoup.nodes.Document): String {
        return runCatching {
            document.baseUri().let { base ->
                java.net.URI(base).resolve(this).toString()
            }
        }.getOrDefault(this)
    }
}
