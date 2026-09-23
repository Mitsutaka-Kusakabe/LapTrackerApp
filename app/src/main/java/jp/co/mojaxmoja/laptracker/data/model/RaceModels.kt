package jp.co.mojaxmoja.laptracker.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Runner(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String
)

data class LapRecord(
    val lapIndex: Int, // 1-based index
    val checkpointMeter: Int, // Distance e.g. 400
    val label: String, // e.g. "400m"
    val lapTimeMillis: Long, // Duration of this lap
    val splitTimeMillis: Long // Total elapsed time at this lap
)

data class RaceRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val runnerName: String,
    val competitionName: String,
    val dateString: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
    val eventName: String, // e.g., "1500m"
    val totalDistanceMeters: Int,
    val totalTimeMillis: Long,
    val laps: List<LapRecord>,
    val notes: String = ""
) {
    fun getFormattedTotalTime(): String = formatMillis(totalTimeMillis)
}

fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = millis % 1000
    return String.format(Locale.getDefault(), "%02d:%02d.%03d", minutes, seconds, ms)
}

fun formatMillisDiff(diffMillis: Long): String {
    val prefix = if (diffMillis > 0) "+" else "-"
    val absVal = kotlin.math.abs(diffMillis)
    val totalSec = absVal / 1000
    val sec = totalSec % 60
    val ms = absVal % 1000
    return if (absVal >= 60000) {
        val min = totalSec / 60
        String.format(Locale.getDefault(), "%s%d:%02d.%03d", prefix, min, sec, ms)
    } else {
        String.format(Locale.getDefault(), "%s%d.%03ds", prefix, sec, ms)
    }
}
