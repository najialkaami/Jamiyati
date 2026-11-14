package com.roma.jamiyati.data.local

import androidx.room.TypeConverter

enum class Frequency(val labelAr: String) {
    WEEKLY("أسبوعية"),
    MONTHLY("شهرية")
}

class Converters {
    @TypeConverter
    fun fromFrequency(value: Frequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): Frequency =
        Frequency.entries.firstOrNull { it.name == value } ?: Frequency.MONTHLY
}
