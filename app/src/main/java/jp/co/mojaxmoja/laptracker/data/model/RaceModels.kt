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

    fun toJson(): org.json.JSONObject {
        val json = org.json.JSONObject()
        json.put("id", id)
        json.put("runnerName", runnerName)
        json.put("competitionName", competitionName)
        json.put("dateString", dateString)
        json.put("eventName", eventName)
        json.put("totalDistanceMeters", totalDistanceMeters)
        json.put("totalTimeMillis", totalTimeMillis)
        json.put("notes", notes)
        val lapsArray = org.json.JSONArray()
        for (lap in laps) {
            val lapJson = org.json.JSONObject()
            lapJson.put("lapIndex", lap.lapIndex)
            lapJson.put("checkpointMeter", lap.checkpointMeter)
            lapJson.put("label", lap.label)
            lapJson.put("lapTimeMillis", lap.lapTimeMillis)
            lapJson.put("splitTimeMillis", lap.splitTimeMillis)
            lapsArray.put(lapJson)
        }
        json.put("laps", lapsArray)
        return json
    }

    companion object {
        fun fromJson(json: org.json.JSONObject): RaceRecord {
            val lapsList = mutableListOf<LapRecord>()
            val lapsArray = json.optJSONArray("laps")
            if (lapsArray != null) {
                for (i in 0 until lapsArray.length()) {
                    val lapObj = lapsArray.getJSONObject(i)
                    lapsList.add(
                        LapRecord(
                            lapIndex = lapObj.optInt("lapIndex", i + 1),
                            checkpointMeter = lapObj.optInt("checkpointMeter", 0),
                            label = lapObj.optString("label", ""),
                            lapTimeMillis = lapObj.optLong("lapTimeMillis", 0L),
                            splitTimeMillis = lapObj.optLong("splitTimeMillis", 0L)
                        )
                    )
                }
            }
            return RaceRecord(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                runnerName = json.optString("runnerName", ""),
                competitionName = json.optString("competitionName", ""),
                dateString = json.optString("dateString", ""),
                eventName = json.optString("eventName", ""),
                totalDistanceMeters = json.optInt("totalDistanceMeters", 0),
                totalTimeMillis = json.optLong("totalTimeMillis", 0L),
                laps = lapsList,
                notes = json.optString("notes", "")
            )
        }
    }
}

fun recalculateLaps(laps: List<LapRecord>): List<LapRecord> {
    var previousSplit = 0L
    return laps.mapIndexed { index, lap ->
        val newLapTime = (lap.splitTimeMillis - previousSplit).coerceAtLeast(0L)
        previousSplit = lap.splitTimeMillis
        lap.copy(
            lapIndex = index + 1,
            lapTimeMillis = newLapTime
        )
    }
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

