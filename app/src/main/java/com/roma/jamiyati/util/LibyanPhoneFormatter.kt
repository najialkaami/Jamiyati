package com.roma.jamiyati.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object LibyanPhoneFormatter {

    /**
     * يطبع رقم الهاتف الليبي بصيغة دولية موحدة (+2189XXXXXXXX)
     */
    fun formatToLibyanInternational(rawPhone: String): String {
        val digitsOnly = rawPhone.filter { it.isDigit() }
        if (digitsOnly.isBlank()) return ""

        val national = when {
            digitsOnly.startsWith("00218") -> digitsOnly.removePrefix("00218")
            digitsOnly.startsWith("218") -> digitsOnly.removePrefix("218")
            digitsOnly.startsWith("0") -> digitsOnly.removePrefix("0")
            else -> digitsOnly
        }
        return "+218$national"
    }

    /**
     * يُرجع الرقم بدون علامة + لاستخدامه في روابط wa.me
     */
    fun formatForWaMe(rawPhone: String): String {
        return formatToLibyanInternational(rawPhone).removePrefix("+")
    }

    fun buildWhatsAppReminderMessage(
        memberName: String,
        circleName: String,
        roundNumber: Int,
        installmentAmount: Double
    ): String {
        val formattedAmount = CurrencyFormatter.formatLyd(installmentAmount)
        return buildString {
            append("السلام عليكم أخي/أختي ")
            append(memberName)
            append("،\n")
            append("نذكّركم بسداد قسط جمعية «")
            append(circleName)
            append("» للجولة رقم ")
            append(roundNumber)
            append(" وقدره ")
            append(formattedAmount)
            append(".\n")
            append("جزاكم الله خيراً.")
        }
    }

    fun openWhatsAppReminder(
        context: Context,
        rawPhone: String,
        memberName: String,
        circleName: String,
        roundNumber: Int,
        installmentAmount: Double
    ): Boolean {
        val waNumber = formatForWaMe(rawPhone)
        if (waNumber.length < 10) return false

        val message = buildWhatsAppReminderMessage(
            memberName = memberName,
            circleName = circleName,
            roundNumber = roundNumber,
            installmentAmount = installmentAmount
        )
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val uri = Uri.parse("https://wa.me/$waNumber?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
