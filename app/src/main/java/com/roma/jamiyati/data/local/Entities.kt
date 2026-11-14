package com.roma.jamiyati.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "circles")
data class CircleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val installmentAmount: Double,
    val frequency: Frequency,
    val startDateEpochDay: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    /** بعد اعتماد ترتيب القبض أو إجراء القرعة يصبح الترتيب نهائياً ولا يمكن تغييره */
    @ColumnInfo(defaultValue = "0") val orderLocked: Boolean = false
)

@Entity(
    tableName = "members",
    foreignKeys = [
        ForeignKey(
            entity = CircleEntity::class,
            parentColumns = ["id"],
            childColumns = ["circleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["circleId"])]
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val circleId: Long,
    val name: String,
    val phone: String,
    val payoutOrder: Int
)

@Entity(
    tableName = "round_payments",
    foreignKeys = [
        ForeignKey(
            entity = CircleEntity::class,
            parentColumns = ["id"],
            childColumns = ["circleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["circleId"]),
        Index(value = ["memberId"]),
        Index(value = ["circleId", "roundNumber", "memberId"], unique = true)
    ]
)
data class RoundPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val circleId: Long,
    val roundNumber: Int,
    val memberId: Long,
    val isPaid: Boolean,
    val paidDateEpochDay: Long? = null
)

data class CircleWithDetails(
    @Embedded val circle: CircleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "circleId"
    )
    val members: List<MemberEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "circleId"
    )
    val payments: List<RoundPaymentEntity>
)
