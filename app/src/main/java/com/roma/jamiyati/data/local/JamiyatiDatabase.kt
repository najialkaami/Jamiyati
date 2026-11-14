package com.roma.jamiyati.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CircleEntity::class,
        MemberEntity::class,
        RoundPaymentEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class JamiyatiDatabase : RoomDatabase() {
    abstract fun jamiyatiDao(): JamiyatiDao

    companion object {
        /** إضافة عمود قفل الترتيب دون حذف بيانات المستخدم */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE circles ADD COLUMN orderLocked INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: JamiyatiDatabase? = null

        fun getInstance(context: Context): JamiyatiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JamiyatiDatabase::class.java,
                    "jamiyati_offline.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
