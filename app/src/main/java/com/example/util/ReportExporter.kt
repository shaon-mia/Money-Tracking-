package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.FinanceTransaction
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatMoney
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

object ReportExporter {

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun copyUriToInternalReceipts(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val target = File(dir, "receipt_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(target).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun buildCsvContent(
        transactions: List<FinanceTransaction>,
        currencyCode: String,
        language: String
    ): String {
        val sb = StringBuilder("\uFEFF") // UTF-8 BOM for Bengali support in Excel/Sheets
        sb.appendLine("Date,Type,Category,Amount ($currencyCode),Payment Method,Note")
        transactions.forEach { tx ->
            val dateStr = formatDateTime(tx.timestampMillis, language).replace(",", " ")
            val cat = tx.categoryDisplayName(language).replace("\"", "\"\"")
            val note = tx.note.replace("\"", "\"\"").replace("\n", " ")
            sb.appendLine("\"$dateStr\",\"${tx.type}\",\"$cat\",${tx.amount},\"${tx.paymentMethod}\",\"$note\"")
        }
        return sb.toString()
    }

    fun writePdfReportToFile(
        context: Context,
        title: String,
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netSavings: Double,
        transactions: List<FinanceTransaction>,
        currencyCode: String,
        language: String
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "MoneyTracker_Report_${System.currentTimeMillis()}.pdf")

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#FF6B2C")
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1A1A1D")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 10.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = 48f

        canvas.drawText("MoneyTracker - $title", margin, y, titlePaint)
        y += 20f
        canvas.drawText(periodLabel, margin, y, mutedPaint)
        y += 18f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 22f

        canvas.drawText("Income: ${formatMoney(totalIncome, currencyCode)}", margin, y, headerPaint)
        canvas.drawText("Expense: ${formatMoney(totalExpense, currencyCode)}", margin + 180f, y, headerPaint)
        canvas.drawText("Savings: ${formatMoney(netSavings, currencyCode)}", margin + 360f, y, headerPaint)
        y += 22f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        canvas.drawText("Transactions (${transactions.size})", margin, y, headerPaint)
        y += 18f

        transactions.forEach { tx ->
            if (y > pageHeight - 48f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = 48f
            }
            val dateStr = formatDateTime(tx.timestampMillis, language)
            val sign = if (tx.type == "income") "+" else "-"
            val amt = "$sign${formatMoney(tx.amount, currencyCode)}"
            val line = "$dateStr | ${tx.categoryDisplayName(language)} | $amt"
            canvas.drawText(line.take(78), margin, y, bodyPaint)
            y += 14f
            if (tx.note.isNotBlank()) {
                canvas.drawText("  Note: ${tx.note.take(72)}", margin, y, mutedPaint)
                y += 13f
            }
            y += 4f
        }

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun writeTextFileToCache(context: Context, fileName: String, content: String): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
