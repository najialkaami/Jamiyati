package com.roma.jamiyati.util

import com.roma.jamiyati.data.local.CircleWithDetails
import com.roma.jamiyati.data.local.Frequency
import com.roma.jamiyati.data.local.MemberEntity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RoundStatus(val labelAr: String) {
    UPCOMING("قادمة"),
    CURRENT("الحالية"),
    DONE("مكتملة")
}

data class RoundScheduleItem(
    val roundNumber: Int,
    val dateEpochDay: Long,
    val receiver: MemberEntity?,
    val totalAmount: Double,
    val paidCount: Int,
    val totalMembers: Int,
    val status: RoundStatus
)

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale("en")).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val decimalFormat = DecimalFormat("#,##0.##", symbols)

    fun formatLyd(amount: Double): String {
        return "${decimalFormat.format(amount)} د.ل"
    }
}

object DateFormatterAr {
    private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("en"))

    fun formatEpochDay(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(formatter)
    }
}

object RoundCalculator {

    fun computeRoundDate(
        startDateEpochDay: Long,
        frequency: Frequency,
        roundNumber: Int
    ): LocalDate {
        val start = LocalDate.ofEpochDay(startDateEpochDay)
        val offset = (roundNumber - 1).coerceAtLeast(0).toLong()
        return when (frequency) {
            Frequency.WEEKLY -> start.plusWeeks(offset)
            Frequency.MONTHLY -> start.plusMonths(offset)
        }
    }

    /**
     * يحسب رقم الجولة الحالية بناءً على تاريخ البداية وتاريخ اليوم، أو يتقدم إذا كانت جميع الجولات السابقة مدفوعة
     */
    fun computeCurrentRoundNumber(
        details: CircleWithDetails,
        today: LocalDate = LocalDate.now()
    ): Int {
        val totalRounds = details.members.size
        if (totalRounds == 0) return 0

        val start = LocalDate.ofEpochDay(details.circle.startDateEpochDay)
        if (today.isBefore(start)) return 1

        var currentByDate = 1
        for (round in 1..totalRounds) {
            val roundDate = computeRoundDate(
                startDateEpochDay = details.circle.startDateEpochDay,
                frequency = details.circle.frequency,
                roundNumber = round
            )
            if (!today.isBefore(roundDate)) {
                currentByDate = round
            }
        }
        return currentByDate.coerceIn(1, totalRounds)
    }

    fun buildSchedule(
        details: CircleWithDetails,
        today: LocalDate = LocalDate.now()
    ): List<RoundScheduleItem> {
        val sortedMembers = details.members.sortedBy { it.payoutOrder }
        val totalMembers = sortedMembers.size
        if (totalMembers == 0) return emptyList()

        val totalPayout = details.circle.installmentAmount * totalMembers
        val currentRound = computeCurrentRoundNumber(details, today)

        return (1..totalMembers).map { roundNum ->
            val roundDate = computeRoundDate(
                startDateEpochDay = details.circle.startDateEpochDay,
                frequency = details.circle.frequency,
                roundNumber = roundNum
            )
            val receiver = sortedMembers.firstOrNull { it.payoutOrder == roundNum }
                ?: sortedMembers.getOrNull(roundNum - 1)
            val paidCount = details.payments.count { it.roundNumber == roundNum && it.isPaid }
            val allPaid = paidCount >= totalMembers

            val status = when {
                roundNum < currentRound && allPaid -> RoundStatus.DONE
                roundNum == currentRound && allPaid && today.isAfter(roundDate) -> RoundStatus.DONE
                roundNum < currentRound -> RoundStatus.DONE
                roundNum == currentRound -> RoundStatus.CURRENT
                else -> RoundStatus.UPCOMING
            }

            RoundScheduleItem(
                roundNumber = roundNum,
                dateEpochDay = roundDate.toEpochDay(),
                receiver = receiver,
                totalAmount = totalPayout,
                paidCount = paidCount,
                totalMembers = totalMembers,
                status = status
            )
        }
    }

    fun getLateMembersForRound(
        details: CircleWithDetails,
        roundNumber: Int
    ): List<MemberEntity> {
        if (roundNumber <= 0) return emptyList()
        val paidMemberIds = details.payments
            .filter { it.roundNumber == roundNumber && it.isPaid }
            .map { it.memberId }
            .toSet()

        return details.members
            .sortedBy { it.payoutOrder }
            .filter { it.id !in paidMemberIds }
    }
}
