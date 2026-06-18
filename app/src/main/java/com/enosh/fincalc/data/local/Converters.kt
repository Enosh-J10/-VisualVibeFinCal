package com.enosh.fincalc.data.local

import androidx.room.TypeConverter
import com.enosh.fincalc.data.model.AiProvider
import com.enosh.fincalc.data.model.MessageRole

class Converters {
    @TypeConverter
    fun fromAiProvider(value: AiProvider): String = value.name

    @TypeConverter
    fun toAiProvider(value: String): AiProvider = AiProvider.valueOf(value)

    @TypeConverter
    fun fromMessageRole(value: MessageRole): String = value.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole = MessageRole.valueOf(value)
}
