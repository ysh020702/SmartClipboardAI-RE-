package com.samsung.smartclipboard.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class PdfSection(
    val title: String,
    val content: String
)

@Singleton
class PdfExportManager @Inject constructor(
    @ApplicationContext
    private val context: Context
) {
    suspend fun generatePdf(
        fileName: String,
        reportTitle: String,
        sections: List<PdfSection>
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            require(sections.isNotEmpty()) {
                "PDF에 포함할 내용이 없습니다."
            }

            val document = PdfDocument()

            try {
                val pageWidth = 595
                val pageHeight = 842
                val margin = 40f
                val bottomLimit = pageHeight - margin
                val contentWidth =
                    pageWidth - (margin * 2).toInt()

                val reportTitlePaint = TextPaint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = Color.BLACK
                    textSize = 24f
                    isFakeBoldText = true
                }

                val sectionTitlePaint = TextPaint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = Color.BLACK
                    textSize = 17f
                    isFakeBoldText = true
                }

                val bodyPaint = TextPaint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = Color.DKGRAY
                    textSize = 13f
                }

                var pageNumber = 0
                var currentPage: PdfDocument.Page? = null
                var currentY = margin

                fun startNewPage() {
                    currentPage?.let {
                        document.finishPage(it)
                    }

                    pageNumber += 1

                    val pageInfo =
                        PdfDocument.PageInfo.Builder(
                            pageWidth,
                            pageHeight,
                            pageNumber
                        ).create()

                    currentPage =
                        document.startPage(pageInfo)

                    currentY = margin
                }

                fun drawLayout(
                    text: String,
                    paint: TextPaint,
                    spacingAfter: Float
                ) {
                    val safeText =
                        text.ifBlank { " " }

                    val layout =
                        StaticLayout.Builder.obtain(
                            safeText,
                            0,
                            safeText.length,
                            paint,
                            contentWidth
                        )
                            .setIncludePad(false)
                            .setLineSpacing(1.5f, 1.05f)
                            .build()

                    if (
                        currentY + layout.height >
                        bottomLimit
                    ) {
                        startNewPage()
                    }

                    val canvas =
                        requireNotNull(currentPage).canvas

                    canvas.save()
                    canvas.translate(margin, currentY)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY +=
                        layout.height + spacingAfter
                }

                fun drawBody(content: String) {
                    val paragraphs =
                        content.split("\n").flatMap { line ->
                            if (line.length > 400) {
                                line.chunked(400)
                            } else {
                                listOf(line)
                            }
                        }

                    paragraphs.forEach { paragraph ->
                        drawLayout(
                            text = paragraph,
                            paint = bodyPaint,
                            spacingAfter = 4f
                        )
                    }

                    currentY += 20f
                }

                startNewPage()

                drawLayout(
                    text = reportTitle.ifBlank {
                        "그때그거 AI 작업 결과"
                    },
                    paint = reportTitlePaint,
                    spacingAfter = 30f
                )

                sections.forEach { section ->
                    drawLayout(
                        text = section.title,
                        paint = sectionTitlePaint,
                        spacingAfter = 10f
                    )

                    drawBody(section.content)
                }

                currentPage?.let {
                    document.finishPage(it)
                }

                val pdfDirectory =
                    File(context.cacheDir, "pdfs").apply {
                        mkdirs()
                    }

                val safeName =
                    sanitizeFileName(fileName)

                val pdfFile =
                    File(pdfDirectory, "$safeName.pdf")

                FileOutputStream(pdfFile).use { output ->
                    document.writeTo(output)
                }

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
            } finally {
                document.close()
            }
        }
    }

    fun sharePdf(
        pdfUri: Uri,
        chooserTitle: String = "PDF 리포트 공유"
    ): Result<Unit> {
        return runCatching {
            val sendIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            val chooser =
                Intent.createChooser(
                    sendIntent,
                    chooserTitle
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            context.startActivity(chooser)
        }

    }

    private fun sanitizeFileName(
        fileName: String
    ): String {
        return fileName
            .replace(
                Regex("""[\/:*?"<>|]"""),
                "_"
            )
            .trim()
            .take(80)
            .ifBlank { "AI_Report" }
    }

}
