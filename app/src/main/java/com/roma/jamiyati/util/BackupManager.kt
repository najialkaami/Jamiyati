package com.roma.jamiyati.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.roma.jamiyati.data.local.CircleWithDetails
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAtEpochDay: Long,
    val circles: List<BackupCircle>
)

@Serializable
data class BackupCircle(
    val name: String,
    val installmentAmount: Double,
    val frequency: String,
    val startDateEpochDay: Long,
    val createdAtMillis: Long,
    val orderLocked: Boolean = false,
    val members: List<BackupMember>,
    val payments: List<BackupPayment>
)

@Serializable
data class BackupMember(
    val originalId: Long,
    val name: String,
    val phone: String,
    val payoutOrder: Int
)

@Serializable
data class BackupPayment(
    val roundNumber: Int,
    val originalMemberId: Long,
    val isPaid: Boolean,
    val paidDateEpochDay: Long? = null
)

object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun serializeCircles(circles: List<CircleWithDetails>): String {
        val payload = BackupPayload(
            version = 1,
            exportedAtEpochDay = LocalDate.now().toEpochDay(),
            circles = circles.map { details ->
                BackupCircle(
                    name = details.circle.name,
                    installmentAmount = details.circle.installmentAmount,
                    frequency = details.circle.frequency.name,
                    startDateEpochDay = details.circle.startDateEpochDay,
                    createdAtMillis = details.circle.createdAtMillis,
                    orderLocked = details.circle.orderLocked,
                    members = details.members.map { m ->
                        BackupMember(
                            originalId = m.id,
                            name = m.name,
                            phone = m.phone,
                            payoutOrder = m.payoutOrder
                        )
                    },
                    payments = details.payments.map { p ->
                        BackupPayment(
                            roundNumber = p.roundNumber,
                            originalMemberId = p.memberId,
                            isPaid = p.isPaid,
                            paidDateEpochDay = p.paidDateEpochDay
                        )
                    }
                )
            }
        )
        return json.encodeToString(payload)
    }

    fun exportAndShareBackup(context: Context, circles: List<CircleWithDetails>): Result<Unit> {
        return runCatching {
            val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val dateStr = DateFormatterAr.formatEpochDay(LocalDate.now().toEpochDay()).replace("/", "-")
            val outFile = File(backupsDir, "jamiyati_backup_$dateStr.json")
            outFile.writeText(serializeCircles(circles), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - تطبيق جمعيتي")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun parseBackupFromUri(context: Context, uri: Uri): Result<BackupPayload> {
        return runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            } ?: error("تعذر قراءة الملف المختار")
            json.decodeFromString<BackupPayload>(raw)
        }
    }
}
