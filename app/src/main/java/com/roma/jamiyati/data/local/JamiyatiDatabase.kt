package com.roma.jamiyati.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CircleEntity::class,
        MemberEntity::class,
        RoundPaymentEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class JamiyatiDatabase : RoomDatabase() {
    abstract fun jamiyatiDao(): JamiyatiDao

    companion object {
        @Volatile
        private var INSTANCE: JamiyatiDatabase? = null

        fun getInstance(context: Context): JamiyatiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JamiyatiDatabase::class.java,
                    "jamiyati_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
