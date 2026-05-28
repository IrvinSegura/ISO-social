package com.nexus.feature.feed.data.local.db

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromStringList(value: String): List<String> =
        try {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }

    @TypeConverter
    fun toStringList(list: List<String>): String =
        JSONArray(list).toString()

    @TypeConverter
    fun fromStringIntMap(value: String): Map<String, Int> =
        try {
            val obj = JSONObject(value)
            obj.keys().asSequence().associateWith { key -> obj.getInt(key) }
        } catch (_: Exception) {
            emptyMap()
        }

    @TypeConverter
    fun toStringIntMap(map: Map<String, Int>): String =
        JSONObject(map as Map<*, *>).toString()
}
