package com.manav.mindguardai.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A small local-only store for this student demo. No account or network is used. */
data class CheckInEntry(
    val timestamp: Long,
    val mood: Int,
    val stress: Int,
    val sleepHours: Int,
    val energy: Int
) {
    fun summary(): String = "Mood $mood/5  ·  Stress $stress/10  ·  Sleep ${sleepHours}h  ·  Energy $energy/10"

    fun dayLabel(): String = SimpleDateFormat("EE", Locale.getDefault()).format(Date(timestamp))
    fun dateLabel(): String = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(timestamp))
}

data class WellbeingSnapshot(val entries: List<CheckInEntry>, val totalCount: Int = entries.size) {
    val latest: CheckInEntry? get() = entries.lastOrNull()
    val count: Int get() = totalCount
}

object WellbeingStore {
    private const val PREFS = "mindguard_local_store_v2"
    private const val HISTORY = "checkin_history"
    private const val FAMILY_OPT_IN = "family_opt_in"
    private const val TOTAL_COUNT = "total_checkin_count"
    private const val MAX_HISTORY = 30

    fun load(context: Context): WellbeingSnapshot {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = preferences.getString(HISTORY, "").orEmpty()
        val entries = raw.split(';').mapNotNull(::decode).takeLast(MAX_HISTORY)
        val totalCount = preferences.getInt(TOTAL_COUNT, entries.size).coerceAtLeast(entries.size)
        return WellbeingSnapshot(entries, totalCount)
    }

    fun saveCheckIn(context: Context, entry: CheckInEntry): WellbeingSnapshot {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = load(context)
        val entries = (previous.entries + entry).takeLast(MAX_HISTORY)
        val totalCount = previous.count + 1
        preferences.edit()
            .putString(HISTORY, entries.joinToString(";") { encode(it) })
            .putInt(TOTAL_COUNT, totalCount)
            .apply()
        return WellbeingSnapshot(entries, totalCount)
    }

    fun isFamilyOptedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(FAMILY_OPT_IN, false)

    fun setFamilyOptIn(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(FAMILY_OPT_IN, enabled)
            .apply()
    }

    private fun encode(entry: CheckInEntry): String = listOf(
        entry.timestamp, entry.mood, entry.stress, entry.sleepHours, entry.energy
    ).joinToString(",")

    private fun decode(line: String): CheckInEntry? {
        if (line.isBlank()) return null
        val parts = line.split(',')
        if (parts.size != 5) return null
        return runCatching {
            CheckInEntry(
                timestamp = parts[0].toLong(),
                mood = parts[1].toInt().coerceIn(1, 5),
                stress = parts[2].toInt().coerceIn(1, 10),
                sleepHours = parts[3].toInt().coerceIn(0, 12),
                energy = parts[4].toInt().coerceIn(1, 10)
            )
        }.getOrNull()
    }
}
