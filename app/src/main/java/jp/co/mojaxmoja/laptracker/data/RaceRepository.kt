package jp.co.mojaxmoja.laptracker.data

import android.content.Context
import jp.co.mojaxmoja.laptracker.data.local.LapTrackerDbHelper
import jp.co.mojaxmoja.laptracker.data.model.CheckpointPreset
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.Runner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File

object RaceRepository {
    private var dbHelper: LapTrackerDbHelper? = null
    private const val LEGACY_FILE_NAME = "races_history.json"

    private val _runners = MutableStateFlow<List<Runner>>(emptyList())
    val runners: StateFlow<List<Runner>> = _runners.asStateFlow()

    private val _races = MutableStateFlow<List<RaceRecord>>(emptyList())
    val races: StateFlow<List<RaceRecord>> = _races.asStateFlow()

    fun init(context: Context) {
        val helper = LapTrackerDbHelper(context.applicationContext)
        dbHelper = helper

        // Initialize presets persistence
        CheckpointPreset.init(helper)

        // Migrate legacy JSON file if exists
        migrateLegacyJson(context, helper)

        // Load runners and races from SQLite
        val loadedRunners = helper.getAllRunners()
        _runners.value = loadedRunners

        val loadedRaces = helper.getAllRaces()
        _races.value = loadedRaces
    }

    private fun migrateLegacyJson(context: Context, helper: LapTrackerDbHelper) {
        try {
            val file = File(context.filesDir, LEGACY_FILE_NAME)
            if (file.exists()) {
                val content = file.readText()
                val array = JSONArray(content)
                val existingIds = helper.getAllRaces().map { it.id }.toSet()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val race = RaceRecord.fromJson(obj)
                    if (!existingIds.contains(race.id)) {
                        helper.insertRace(race)
                    }
                }
                // Once migrated, delete legacy file safely
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addRunner(name: String): Runner {
        val newRunner = Runner(name = name)
        _runners.value = _runners.value + newRunner
        dbHelper?.insertRunner(newRunner)
        return newRunner
    }

    fun saveRace(race: RaceRecord) {
        dbHelper?.insertRace(race)
        _races.value = listOf(race) + _races.value.filter { it.id != race.id }
    }

    fun updateRace(updatedRace: RaceRecord) {
        dbHelper?.updateRace(updatedRace)
        _races.value = _races.value.map {
            if (it.id == updatedRace.id) updatedRace else it
        }
    }

    fun deleteRace(id: String) {
        dbHelper?.deleteRace(id)
        _races.value = _races.value.filter { it.id != id }
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
