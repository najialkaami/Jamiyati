package com.roma.jamiyati.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JamiyatiDao {

    @Transaction
    @Query("SELECT * FROM circles ORDER BY createdAtMillis DESC")
    fun observeAllCirclesWithDetails(): Flow<List<CircleWithDetails>>

    @Transaction
    @Query("SELECT * FROM circles WHERE id = :circleId")
    fun observeCircleWithDetails(circleId: Long): Flow<CircleWithDetails?>

    @Transaction
    @Query("SELECT * FROM circles ORDER BY createdAtMillis DESC")
    suspend fun getAllCirclesWithDetailsOnce(): List<CircleWithDetails>

    @Transaction
    @Query("SELECT * FROM circles WHERE id = :circleId")
    suspend fun getCircleWithDetailsOnce(circleId: Long): CircleWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCircle(circle: CircleEntity): Long

    @Update
    suspend fun updateCircle(circle: CircleEntity)

    @Delete
    suspend fun deleteCircle(circle: CircleEntity)

    @Query("DELETE FROM circles WHERE id = :circleId")
    suspend fun deleteCircleById(circleId: Long)

    @Query("DELETE FROM circles")
    suspend fun deleteAllCircles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>): List<Long>

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Update
    suspend fun updateMembers(members: List<MemberEntity>)

    @Query("DELETE FROM members WHERE id IN (:memberIds)")
    suspend fun deleteMembersByIds(memberIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(payment: RoundPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<RoundPaymentEntity>)

    @Query("SELECT * FROM round_payments WHERE circleId = :circleId AND roundNumber = :roundNumber AND memberId = :memberId LIMIT 1")
    suspend fun findPayment(circleId: Long, roundNumber: Int, memberId: Long): RoundPaymentEntity?

    @Query("DELETE FROM round_payments WHERE circleId = :circleId AND roundNumber > :maxRound")
    suspend fun deletePaymentsAboveRound(circleId: Long, maxRound: Int)
}
