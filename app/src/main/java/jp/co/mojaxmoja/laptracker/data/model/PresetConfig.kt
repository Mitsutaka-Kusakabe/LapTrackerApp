package jp.co.mojaxmoja.laptracker.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CheckpointPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val eventName: String,
    val totalDistanceMeters: Int,
    val checkpoints: List<Int>, // Distance in meters at each split checkpoint
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            CheckpointPreset(
                id = "preset-800m",
                eventName = "800m",
                totalDistanceMeters = 800,
                checkpoints = listOf(400, 800)
            ),
            CheckpointPreset(
                id = "preset-1500m",
                eventName = "1500m",
                totalDistanceMeters = 1500,
                checkpoints = listOf(400, 800, 1000, 1200, 1500)
            ),
            CheckpointPreset(
                id = "preset-3000m",
                eventName = "3000m",
                totalDistanceMeters = 3000,
                checkpoints = listOf(400, 800, 1000, 1200, 1600, 2000, 2400, 2800, 3000)
            ),
            CheckpointPreset(
                id = "preset-5000m",
                eventName = "5000m",
                totalDistanceMeters = 5000,
                checkpoints = listOf(400, 800, 1000, 1200, 1600, 2000, 2400, 2800, 3200, 3600, 4000, 4400, 4800, 5000)
            )
        )

        private val _allPresets = MutableStateFlow<List<CheckpointPreset>>(DEFAULT_PRESETS)
        val allPresets: StateFlow<List<CheckpointPreset>> = _allPresets.asStateFlow()
        private var dbHelper: jp.co.mojaxmoja.laptracker.data.local.LapTrackerDbHelper? = null

        fun init(helper: jp.co.mojaxmoja.laptracker.data.local.LapTrackerDbHelper) {
            dbHelper = helper
            val customPresets = helper.getAllCustomPresets()
            _allPresets.value = DEFAULT_PRESETS + customPresets
        }

        fun addCustomPreset(eventName: String, checkpoints: List<Int>): CheckpointPreset {
            val totalDist = checkpoints.maxOrNull() ?: 1000
            val sortedCheckpoints = checkpoints.sorted()
            val newPreset = CheckpointPreset(
                eventName = eventName,
                totalDistanceMeters = totalDist,
                checkpoints = sortedCheckpoints,
                isCustom = true
            )
            _allPresets.value = _allPresets.value + newPreset
            dbHelper?.insertCustomPreset(newPreset)
            return newPreset
        }

        fun deleteCustomPreset(id: String) {
            _allPresets.value = _allPresets.value.filter { it.id != id }
            dbHelper?.deleteCustomPreset(id)
        }
    }
}
