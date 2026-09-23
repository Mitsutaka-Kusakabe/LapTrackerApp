package jp.co.mojaxmoja.laptracker.data

import android.content.Context
import jp.co.mojaxmoja.laptracker.data.model.LapRecord
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.Runner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File

object RaceRepository {
    private var appContext: Context? = null
    private const val FILE_NAME = "races_history.json"

    private val sampleRaces = listOf(
        RaceRecord(
            id = "sample-1",
            runnerName = "山田 太郎",
            competitionName = "第1回 日本記録会",
            dateString = "2026-09-10 14:30",
            eventName = "1500m",
            totalDistanceMeters = 1500,
            totalTimeMillis = 238400, // 03:58.40
            laps = listOf(
                LapRecord(1, 400, "400m", 63200, 63200),
                LapRecord(2, 800, "800m", 64100, 127300),
                LapRecord(3, 1000, "1000m", 32000, 159300),
                LapRecord(4, 1200, "1200m", 31800, 191100),
                LapRecord(5, 1500, "1500m", 47300, 238400)
            ),
            notes = "自己ベスト更新！"
        ),
        RaceRecord(
            id = "sample-2",
            runnerName = "山田 太郎",
            competitionName = "春季陸上競技大会",
            dateString = "2026-05-15 11:00",
            eventName = "1500m",
            totalDistanceMeters = 1500,
            totalTimeMillis = 243100, // 04:03.10
            laps = listOf(
                LapRecord(1, 400, "400m", 64500, 64500),
                LapRecord(2, 800, "800m", 65200, 129700),
                LapRecord(3, 1000, "1000m", 33100, 162800),
                LapRecord(4, 1200, "1200m", 32500, 195300),
                LapRecord(5, 1500, "1500m", 47800, 243100)
            ),
            notes = "ラストスパートで少し失速"
        )
    )

    private val _runners = MutableStateFlow<List<Runner>>(
        listOf(
            Runner(name = "山田 太郎"),
            Runner(name = "佐藤 健太")
        )
    )
    val runners: StateFlow<List<Runner>> = _runners.asStateFlow()

    private val _races = MutableStateFlow<List<RaceRecord>>(sampleRaces)
    val races: StateFlow<List<RaceRecord>> = _races.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadFromFile()
    }

    private fun loadFromFile() {
        val context = appContext ?: return
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return

        try {
            val content = file.readText()
            val array = JSONArray(content)
            val loadedRaces = mutableListOf<RaceRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                loadedRaces.add(RaceRecord.fromJson(obj))
            }
            if (loadedRaces.isNotEmpty()) {
                _races.value = loadedRaces
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToFile() {
        val context = appContext ?: return
        try {
            val file = File(context.filesDir, FILE_NAME)
            val array = JSONArray()
            for (race in _races.value) {
                array.put(race.toJson())
            }
            file.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addRunner(name: String): Runner {
        val newRunner = Runner(name = name)
        _runners.value = _runners.value + newRunner
        return newRunner
    }

    fun saveRace(race: RaceRecord) {
        _races.value = listOf(race) + _races.value
        saveToFile()
    }

    fun updateRace(updatedRace: RaceRecord) {
        _races.value = _races.value.map {
            if (it.id == updatedRace.id) updatedRace else it
        }
        saveToFile()
    }

    fun deleteRace(id: String) {
        _races.value = _races.value.filter { it.id != id }
        saveToFile()
    }

    fun getRaceById(id: String): RaceRecord? {
        return _races.value.find { it.id == id }
    }

    fun getRacesByRunnerAndEvent(runnerName: String, eventName: String): List<RaceRecord> {
        return _races.value.filter {
            it.runnerName.equals(runnerName, ignoreCase = true) &&
                    it.eventName.equals(eventName, ignoreCase = true)
        }
    }
}

