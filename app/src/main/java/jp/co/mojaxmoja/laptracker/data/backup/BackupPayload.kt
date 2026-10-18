package jp.co.mojaxmoja.laptracker.data.backup

import jp.co.mojaxmoja.laptracker.data.model.CheckpointPreset
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.Runner
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val runners: List<Runner>,
    val races: List<RaceRecord>,
    val customPresets: List<CheckpointPreset>
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportedAtMillis", exportedAtMillis)

        // Runners
        val runnersArray = JSONArray()
        for (runner in runners) {
            val rObj = JSONObject()
            rObj.put("id", runner.id)
            rObj.put("name", runner.name)
            runnersArray.put(rObj)
        }
        root.put("runners", runnersArray)

        // Races
        val racesArray = JSONArray()
        for (race in races) {
            racesArray.put(race.toJson())
        }
        root.put("races", racesArray)

        // Custom Presets
        val presetsArray = JSONArray()
        for (preset in customPresets) {
            val pObj = JSONObject()
            pObj.put("id", preset.id)
            pObj.put("eventName", preset.eventName)
            pObj.put("totalDistanceMeters", preset.totalDistanceMeters)
            val cpArray = JSONArray()
            for (cp in preset.checkpoints) {
                cpArray.put(cp)
            }
            pObj.put("checkpoints", cpArray)
            presetsArray.put(pObj)
        }
        root.put("customPresets", presetsArray)

        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): BackupPayload {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val exportedAtMillis = root.optLong("exportedAtMillis", 0L)

            // Runners
            val runnersList = mutableListOf<Runner>()
            val runnersArray = root.optJSONArray("runners")
            if (runnersArray != null) {
                for (i in 0 until runnersArray.length()) {
                    val rObj = runnersArray.getJSONObject(i)
                    runnersList.add(
                        Runner(
                            id = rObj.optString("id", java.util.UUID.randomUUID().toString()),
                            name = rObj.getString("name")
                        )
                    )
                }
            }

            // Races
            val racesList = mutableListOf<RaceRecord>()
            val racesArray = root.optJSONArray("races")
            if (racesArray != null) {
                for (i in 0 until racesArray.length()) {
                    val raceObj = racesArray.getJSONObject(i)
                    racesList.add(RaceRecord.fromJson(raceObj))
                }
            }

            // Custom Presets
            val presetsList = mutableListOf<CheckpointPreset>()
            val presetsArray = root.optJSONArray("customPresets")
            if (presetsArray != null) {
                for (i in 0 until presetsArray.length()) {
                    val pObj = presetsArray.getJSONObject(i)
                    val cpArray = pObj.optJSONArray("checkpoints")
                    val checkpoints = mutableListOf<Int>()
                    if (cpArray != null) {
                        for (j in 0 until cpArray.length()) {
                            checkpoints.add(cpArray.getInt(j))
                        }
                    }
                    presetsList.add(
                        CheckpointPreset(
                            id = pObj.optString("id", java.util.UUID.randomUUID().toString()),
                            eventName = pObj.getString("eventName"),
                            totalDistanceMeters = pObj.getInt("totalDistanceMeters"),
                            checkpoints = checkpoints,
                            isCustom = true
                        )
                    )
                }
            }

            return BackupPayload(
                version = version,
                exportedAtMillis = exportedAtMillis,
                runners = runnersList,
                races = racesList,
                customPresets = presetsList
            )
        }
    }
}
