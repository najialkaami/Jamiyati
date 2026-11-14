package com.roma.jamiyati.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.roma.jamiyati.data.local.CircleWithDetails
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object PdfReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun exportAndShareCircleReport(context: Context, details: CircleWithDetails): Result<Unit> {
        return runCatching {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val safeName = details.circle.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
            val outFile = File(reportsDir, "jamiyati_report_${safeName}_${details.circle.id}.pdf")

            val document = PdfDocument()
            val contentWidth = (PAGE_WIDTH - MARGIN * 2).toInt()

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F5132")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111827")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1F2937")
                textSize = 11.5f
            }
            val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6B7280")
                textSize = 10f
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 1f
            }

            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            var canvas = page.canvas
            var y = MARGIN

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN
                }
            }

            fun drawRtlBlock(text: String, paint: TextPaint, spacingAfter: Float = 8f) {
                val layout = StaticLayout.Builder
                    .obtain(text, 0, text.length, paint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setTextDirection(TextDirectionHeuristics.RTL)
                    .setLineSpacing(2f, 1.15f)
                    .build()

                ensureSpace(layout.height + spacingAfter)
                canvas.save()
                canvas.translate(MARGIN, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + spacingAfter
            }

            val members = details.members.sortedBy { it.payoutOrder }
            val totalPayout = details.circle.installmentAmount * members.size
            val schedule = RoundCalculator.buildSchedule(details)
            val currentRound = RoundCalculator.computeCurrentRoundNumber(details)

            drawRtlBlock("تقرير جمعية: ${details.circle.name}", titlePaint, 4f)
            drawRtlBlock(
                "تاريخ التقرير: ${DateFormatterAr.formatEpochDay(LocalDate.now().toEpochDay())} · الدور الحالي: $currentRound من ${members.size}",
                mutedPaint,
                12f
            )

            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f

            drawRtlBlock("ملخص الجمعية", headingPaint, 6f)
            drawRtlBlock(
                "قيمة القسط: ${CurrencyFormatter.formatLyd(details.circle.installmentAmount)} · الدورية: ${details.circle.frequency.labelAr}",
                bodyPaint,
                4f
            )
            drawRtlBlock(
                "إجمالي القبض: ${CurrencyFormatter.formatLyd(totalPayout)} · عدد المشتركين: ${members.size} · تاريخ البدء: ${DateFormatterAr.formatEpochDay(details.circle.startDateEpochDay)}",
                bodyPaint,
                14f
            )

            drawRtlBlock("ترتيب المشتركين والقبض", headingPaint, 6f)
            members.forEach { member ->
                val intlPhone = LibyanPhoneFormatter.formatForDisplay(member.phone)
                drawRtlBlock(
                    "${member.payoutOrder}. ${member.name} — الهاتف: $intlPhone",
                    bodyPaint,
                    4f
                )
            }
            y += 10f

            drawRtlBlock("جدول الجولات وحالة السداد", headingPaint, 8f)
            schedule.forEach { roundItem ->
                val receiverName = roundItem.receiver?.name ?: "غير محدد"
                val headerLine = "الجولة ${roundItem.roundNumber} (${roundItem.status.labelAr}) · التاريخ: ${DateFormatterAr.formatEpochDay(roundItem.dateEpochDay)} · المستلم: $receiverName · الإجمالي: ${CurrencyFormatter.formatLyd(roundItem.totalAmount)}"
                drawRtlBlock(headerLine, headingPaint.apply { textSize = 11.5f }, 4f)

                val roundPayments = details.payments.filter { it.roundNumber == roundItem.roundNumber }
                members.forEach { member ->
                    val payment = roundPayments.firstOrNull { it.memberId == member.id }
                    val statusText = if (payment?.isPaid == true) {
                        val dateStr = payment.paidDateEpochDay?.let { DateFormatterAr.formatEpochDay(it) } ?: ""
                        "مدفوع ($dateStr)"
                    } else {
                        "غير مدفوع"
                    }
                    drawRtlBlock("   • ${member.name}: $statusText", bodyPaint, 2f)
                }
                y += 8f
            }

            document.finishPage(page)
            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير جمعية ${details.circle.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير الجمعية").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
