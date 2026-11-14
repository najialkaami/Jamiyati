package com.roma.jamiyati.data.repository

import androidx.room.withTransaction
import com.roma.jamiyati.data.local.CircleEntity
import com.roma.jamiyati.data.local.CircleWithDetails
import com.roma.jamiyati.data.local.Frequency
import com.roma.jamiyati.data.local.JamiyatiDao
import com.roma.jamiyati.data.local.JamiyatiDatabase
import com.roma.jamiyati.data.local.MemberEntity
import com.roma.jamiyati.data.local.RoundPaymentEntity
import com.roma.jamiyati.data.preferences.AppPreferences
import com.roma.jamiyati.data.preferences.ThemeMode
import com.roma.jamiyati.util.BackupPayload
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class MemberDraft(
    val id: Long = 0L,
    val name: String,
    val phone: String,
    val payoutOrder: Int
)

class JamiyatiRepository(
    private val dao: JamiyatiDao,
    private val preferences: AppPreferences,
    private val database: JamiyatiDatabase? = null
) {
    fun observeAllCircles(): Flow<List<CircleWithDetails>> = dao.observeAllCirclesWithDetails()

    fun observeCircle(circleId: Long): Flow<CircleWithDetails?> = dao.observeCircleWithDetails(circleId)

    suspend fun getCircleOnce(circleId: Long): CircleWithDetails? = dao.getCircleWithDetailsOnce(circleId)

    suspend fun getAllCirclesOnce(): List<CircleWithDetails> = dao.getAllCirclesWithDetailsOnce()

    val themeModeFlow: Flow<ThemeMode> = preferences.themeModeFlow
    val isPinEnabledFlow: Flow<Boolean> = preferences.isPinEnabledFlow
    val pinHashFlow: Flow<String?> = preferences.pinHashFlow

    suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
    suspend fun enablePin(pin: String) = preferences.enablePin(pin)
    suspend fun disablePin() = preferences.disablePin()

    suspend fun saveCircleWithMembers(
        circleId: Long?,
        name: String,
        installmentAmount: Double,
        frequency: Frequency,
        startDateEpochDay: Long,
        members: List<MemberDraft>
    ): Long {
        val normalizedMembers = members.mapIndexed { index, draft ->
            draft.copy(payoutOrder = index + 1)
        }

        if (circleId == null || circleId == 0L) {
            val newCircleId = dao.insertCircle(
                CircleEntity(
                    name = name.trim(),
                    installmentAmount = installmentAmount,
                    frequency = frequency,
                    startDateEpochDay = startDateEpochDay
                )
            )
            dao.insertMembers(
                normalizedMembers.map { draft ->
                    MemberEntity(
                        circleId = newCircleId,
                        name = draft.name.trim(),
                        phone = draft.phone.trim(),
                        payoutOrder = draft.payoutOrder
                    )
                }
            )
            return newCircleId
        } else {
            val existing = dao.getCircleWithDetailsOnce(circleId)
            val createdAt = existing?.circle?.createdAtMillis ?: System.currentTimeMillis()
            val locked = existing?.circle?.orderLocked ?: false
            dao.updateCircle(
                CircleEntity(
                    id = circleId,
                    name = name.trim(),
                    installmentAmount = installmentAmount,
                    frequency = frequency,
                    startDateEpochDay = startDateEpochDay,
                    createdAtMillis = createdAt,
                    orderLocked = locked
                )
            )

            if (locked && existing != null) {
                // الترتيب معتمد: يُسمح فقط بتعديل الاسم والهاتف، دون إضافة أو حذف أو تغيير الدور
                val edits = normalizedMembers.filter { it.id > 0L }.associateBy { it.id }
                val updated = existing.members.map { m ->
                    val e = edits[m.id]
                    if (e == null) m else m.copy(name = e.name.trim(), phone = e.phone.trim())
                }
                dao.updateMembers(updated)
                return circleId
            }

            val existingMemberIds = existing?.members?.map { it.id }?.toSet() ?: emptySet()
            val keptIds = normalizedMembers.mapNotNull { if (it.id > 0L) it.id else null }.toSet()
            val removedIds = (existingMemberIds - keptIds).toList()
            if (removedIds.isNotEmpty()) {
                dao.deleteMembersByIds(removedIds)
            }

            val toUpdate = normalizedMembers.filter { it.id > 0L }.map {
                MemberEntity(
                    id = it.id,
                    circleId = circleId,
                    name = it.name.trim(),
                    phone = it.phone.trim(),
                    payoutOrder = it.payoutOrder
                )
            }
            if (toUpdate.isNotEmpty()) {
                dao.updateMembers(toUpdate)
            }

            val toInsert = normalizedMembers.filter { it.id == 0L }.map {
                MemberEntity(
                    circleId = circleId,
                    name = it.name.trim(),
                    phone = it.phone.trim(),
                    payoutOrder = it.payoutOrder
                )
            }
            if (toInsert.isNotEmpty()) {
                dao.insertMembers(toInsert)
            }

            dao.deletePaymentsAboveRound(circleId, normalizedMembers.size)
            return circleId
        }
    }

    suspend fun deleteCircle(circleId: Long) {
        dao.deleteCircleById(circleId)
    }

    suspend fun lockOrder(circleId: Long) = dao.lockOrder(circleId)

    suspend fun updateMembersPayoutOrder(members: List<MemberEntity>) {
        val reindexed = members.mapIndexed { index, member ->
            member.copy(payoutOrder = index + 1)
        }
        dao.updateMembers(reindexed)
    }

    suspend fun setPaymentStatus(
        circleId: Long,
        roundNumber: Int,
        memberId: Long,
        isPaid: Boolean,
        paidDateEpochDay: Long? = if (isPaid) LocalDate.now().toEpochDay() else null
    ) {
        val existing = dao.findPayment(circleId, roundNumber, memberId)
        val entity = RoundPaymentEntity(
            id = existing?.id ?: 0L,
            circleId = circleId,
            roundNumber = roundNumber,
            memberId = memberId,
            isPaid = isPaid,
            paidDateEpochDay = if (isPaid) (paidDateEpochDay ?: LocalDate.now().toEpochDay()) else null
        )
        dao.upsertPayment(entity)
    }

    suspend fun restoreFromBackup(payload: BackupPayload, replaceExisting: Boolean) {
        val block: suspend () -> Unit = {
            if (replaceExisting) {
                dao.deleteAllCircles()
            }
            payload.circles.forEach { backupCircle ->
                val newCircleId = dao.insertCircle(
                    CircleEntity(
                        name = backupCircle.name,
                        installmentAmount = backupCircle.installmentAmount,
                        frequency = Frequency.entries.firstOrNull { it.name == backupCircle.frequency }
                            ?: Frequency.MONTHLY,
                        startDateEpochDay = backupCircle.startDateEpochDay,
                        createdAtMillis = backupCircle.createdAtMillis,
                        orderLocked = backupCircle.orderLocked
                    )
                )

                val memberIdMap = mutableMapOf<Long, Long>()
                backupCircle.members.sortedBy { it.payoutOrder }.forEach { bm ->
                    val newMemberId = dao.insertMember(
                        MemberEntity(
                            circleId = newCircleId,
                            name = bm.name,
                            phone = bm.phone,
                            payoutOrder = bm.payoutOrder
                        )
                    )
                    memberIdMap[bm.originalId] = newMemberId
                }

                val restoredPayments = backupCircle.payments.mapNotNull { bp ->
                    val mappedMemberId = memberIdMap[bp.originalMemberId] ?: return@mapNotNull null
                    RoundPaymentEntity(
                        circleId = newCircleId,
                        roundNumber = bp.roundNumber,
                        memberId = mappedMemberId,
                        isPaid = bp.isPaid,
                        paidDateEpochDay = bp.paidDateEpochDay
                    )
                }
                if (restoredPayments.isNotEmpty()) {
                    dao.insertPayments(restoredPayments)
                }
            }
        }

        if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }
}
