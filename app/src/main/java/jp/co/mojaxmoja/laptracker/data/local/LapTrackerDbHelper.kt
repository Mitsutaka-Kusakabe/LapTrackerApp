package jp.co.mojaxmoja.laptracker.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import jp.co.mojaxmoja.laptracker.data.model.CheckpointPreset
import jp.co.mojaxmoja.laptracker.data.model.LapRecord
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.Runner

class LapTrackerDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "lap_tracker.db"
        const val DATABASE_VERSION = 1

        // Table Runners
        const val TABLE_RUNNERS = "runners"
        const val COL_RUNNER_ID = "id"
        const val COL_RUNNER_NAME = "name"

        // Table Races
        const val TABLE_RACES = "races"
        const val COL_RACE_ID = "id"
        const val COL_RACE_RUNNER_NAME = "runner_name"
        const val COL_RACE_COMPETITION_NAME = "competition_name"
        const val COL_RACE_DATE_STRING = "date_string"
        const val COL_RACE_EVENT_NAME = "event_name"
        const val COL_RACE_TOTAL_DISTANCE = "total_distance_meters"
        const val COL_RACE_TOTAL_TIME = "total_time_millis"
        const val COL_RACE_NOTES = "notes"

        // Table Laps
        const val TABLE_LAPS = "laps"
        const val COL_LAP_ID = "id"
        const val COL_LAP_RACE_ID = "race_id"
        const val COL_LAP_INDEX = "lap_index"
        const val COL_LAP_CHECKPOINT_METER = "checkpoint_meter"
        const val COL_LAP_LABEL = "label"
        const val COL_LAP_TIME_MILLIS = "lap_time_millis"
        const val COL_LAP_SPLIT_TIME_MILLIS = "split_time_millis"

        // Table Custom Presets
        const val TABLE_CUSTOM_PRESETS = "custom_presets"
        const val COL_PRESET_ID = "id"
        const val COL_PRESET_EVENT_NAME = "event_name"
        const val COL_PRESET_TOTAL_DISTANCE = "total_distance_meters"
        const val COL_PRESET_CHECKPOINTS_CSV = "checkpoints_csv"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Runners
        db.execSQL(
            """
            CREATE TABLE $TABLE_RUNNERS (
                $COL_RUNNER_ID TEXT PRIMARY KEY,
                $COL_RUNNER_NAME TEXT NOT NULL
            );
            """.trimIndent()
        )

        // Create Races
        db.execSQL(
            """
            CREATE TABLE $TABLE_RACES (
                $COL_RACE_ID TEXT PRIMARY KEY,
                $COL_RACE_RUNNER_NAME TEXT NOT NULL,
                $COL_RACE_COMPETITION_NAME TEXT NOT NULL,
                $COL_RACE_DATE_STRING TEXT NOT NULL,
                $COL_RACE_EVENT_NAME TEXT NOT NULL,
                $COL_RACE_TOTAL_DISTANCE INTEGER NOT NULL,
                $COL_RACE_TOTAL_TIME INTEGER NOT NULL,
                $COL_RACE_NOTES TEXT
            );
            """.trimIndent()
        )

        // Create Laps
        db.execSQL(
            """
            CREATE TABLE $TABLE_LAPS (
                $COL_LAP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LAP_RACE_ID TEXT NOT NULL,
                $COL_LAP_INDEX INTEGER NOT NULL,
                $COL_LAP_CHECKPOINT_METER INTEGER NOT NULL,
                $COL_LAP_LABEL TEXT NOT NULL,
                $COL_LAP_TIME_MILLIS INTEGER NOT NULL,
                $COL_LAP_SPLIT_TIME_MILLIS INTEGER NOT NULL,
                FOREIGN KEY($COL_LAP_RACE_ID) REFERENCES $TABLE_RACES($COL_RACE_ID) ON DELETE CASCADE
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_laps_race_id ON $TABLE_LAPS($COL_LAP_RACE_ID);")

        // Create Custom Presets
        db.execSQL(
            """
            CREATE TABLE $TABLE_CUSTOM_PRESETS (
                $COL_PRESET_ID TEXT PRIMARY KEY,
                $COL_PRESET_EVENT_NAME TEXT NOT NULL,
                $COL_PRESET_TOTAL_DISTANCE INTEGER NOT NULL,
                $COL_PRESET_CHECKPOINTS_CSV TEXT NOT NULL
            );
            """.trimIndent()
        )

        // Seed default runners
        insertInitialRunner(db, "山田 太郎")
        insertInitialRunner(db, "佐藤 健太")

        // Seed sample races
        seedInitialRaces(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Safe migration for future database upgrades without losing existing user records
    }

    private fun insertInitialRunner(db: SQLiteDatabase, name: String) {
        val values = ContentValues().apply {
            put(COL_RUNNER_ID, java.util.UUID.randomUUID().toString())
            put(COL_RUNNER_NAME, name)
        }
        db.insert(TABLE_RUNNERS, null, values)
    }

    private fun seedInitialRaces(db: SQLiteDatabase) {
        // Sample Race 1
        val r1 = RaceRecord(
            id = "sample-1",
            runnerName = "山田 太郎",
            competitionName = "第1回 日本記録会",
            dateString = "2026-09-10 14:30",
            eventName = "1500m",
            totalDistanceMeters = 1500,
            totalTimeMillis = 238400,
            laps = listOf(
                LapRecord(1, 400, "400m", 63200, 63200),
                LapRecord(2, 800, "800m", 64100, 127300),
                LapRecord(3, 1000, "1000m", 32000, 159300),
                LapRecord(4, 1200, "1200m", 31800, 191100),
                LapRecord(5, 1500, "1500m", 47300, 238400)
            ),
            notes = "自己ベスト更新！"
        )
        insertRaceInternal(db, r1)

        // Sample Race 2
        val r2 = RaceRecord(
            id = "sample-2",
            runnerName = "山田 太郎",
            competitionName = "春季陸上競技大会",
            dateString = "2026-05-15 11:00",
            eventName = "1500m",
            totalDistanceMeters = 1500,
            totalTimeMillis = 243100,
            laps = listOf(
                LapRecord(1, 400, "400m", 64500, 64500),
                LapRecord(2, 800, "800m", 65200, 129700),
                LapRecord(3, 1000, "1000m", 33100, 162800),
                LapRecord(4, 1200, "1200m", 32500, 195300),
                LapRecord(5, 1500, "1500m", 47800, 243100)
            ),
            notes = "ラストスパートで少し失速"
        )
        insertRaceInternal(db, r2)
    }

    private fun insertRaceInternal(db: SQLiteDatabase, race: RaceRecord) {
        val raceValues = ContentValues().apply {
            put(COL_RACE_ID, race.id)
            put(COL_RACE_RUNNER_NAME, race.runnerName)
            put(COL_RACE_COMPETITION_NAME, race.competitionName)
            put(COL_RACE_DATE_STRING, race.dateString)
            put(COL_RACE_EVENT_NAME, race.eventName)
            put(COL_RACE_TOTAL_DISTANCE, race.totalDistanceMeters)
            put(COL_RACE_TOTAL_TIME, race.totalTimeMillis)
            put(COL_RACE_NOTES, race.notes)
        }
        db.insert(TABLE_RACES, null, raceValues)

        for (lap in race.laps) {
            val lapValues = ContentValues().apply {
                put(COL_LAP_RACE_ID, race.id)
                put(COL_LAP_INDEX, lap.lapIndex)
                put(COL_LAP_CHECKPOINT_METER, lap.checkpointMeter)
                put(COL_LAP_LABEL, lap.label)
                put(COL_LAP_TIME_MILLIS, lap.lapTimeMillis)
                put(COL_LAP_SPLIT_TIME_MILLIS, lap.splitTimeMillis)
            }
            db.insert(TABLE_LAPS, null, lapValues)
        }
    }

    // --- Public Operations ---

    fun getAllRunners(): List<Runner> {
        val list = mutableListOf<Runner>()
        val db = readableDatabase
        val cursor = db.query(TABLE_RUNNERS, null, null, null, null, null, "$COL_RUNNER_NAME ASC")
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_RUNNER_ID)
            val nameIdx = it.getColumnIndexOrThrow(COL_RUNNER_NAME)
            while (it.moveToNext()) {
                list.add(Runner(id = it.getString(idIdx), name = it.getString(nameIdx)))
            }
        }
        return list
    }

    fun insertRunner(runner: Runner) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_RUNNER_ID, runner.id)
            put(COL_RUNNER_NAME, runner.name)
        }
        db.insertWithOnConflict(TABLE_RUNNERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllRaces(): List<RaceRecord> {
        val races = mutableListOf<RaceRecord>()
        val db = readableDatabase
        // Retrieve races in reverse chronological order (newest first)
        val cursor = db.query(TABLE_RACES, null, null, null, null, null, "$COL_RACE_DATE_STRING DESC, rowid DESC")
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_RACE_ID)
            val runnerIdx = c.getColumnIndexOrThrow(COL_RACE_RUNNER_NAME)
            val compIdx = c.getColumnIndexOrThrow(COL_RACE_COMPETITION_NAME)
            val dateIdx = c.getColumnIndexOrThrow(COL_RACE_DATE_STRING)
            val eventIdx = c.getColumnIndexOrThrow(COL_RACE_EVENT_NAME)
            val distIdx = c.getColumnIndexOrThrow(COL_RACE_TOTAL_DISTANCE)
            val timeIdx = c.getColumnIndexOrThrow(COL_RACE_TOTAL_TIME)
            val notesIdx = c.getColumnIndexOrThrow(COL_RACE_NOTES)

            while (c.moveToNext()) {
                val raceId = c.getString(idIdx)
                val laps = getLapsForRace(db, raceId)
                races.add(
                    RaceRecord(
                        id = raceId,
                        runnerName = c.getString(runnerIdx),
                        competitionName = c.getString(compIdx),
                        dateString = c.getString(dateIdx),
                        eventName = c.getString(eventIdx),
                        totalDistanceMeters = c.getInt(distIdx),
                        totalTimeMillis = c.getLong(timeIdx),
                        laps = laps,
                        notes = c.getString(notesIdx) ?: ""
                    )
                )
            }
        }
        return races
    }

    private fun getLapsForRace(db: SQLiteDatabase, raceId: String): List<LapRecord> {
        val laps = mutableListOf<LapRecord>()
        val cursor = db.query(
            TABLE_LAPS,
            null,
            "$COL_LAP_RACE_ID = ?",
            arrayOf(raceId),
            null,
            null,
            "$COL_LAP_INDEX ASC"
        )
        cursor.use { c ->
            val indexIdx = c.getColumnIndexOrThrow(COL_LAP_INDEX)
            val meterIdx = c.getColumnIndexOrThrow(COL_LAP_CHECKPOINT_METER)
            val labelIdx = c.getColumnIndexOrThrow(COL_LAP_LABEL)
            val lapTimeIdx = c.getColumnIndexOrThrow(COL_LAP_TIME_MILLIS)
            val splitTimeIdx = c.getColumnIndexOrThrow(COL_LAP_SPLIT_TIME_MILLIS)

            while (c.moveToNext()) {
                laps.add(
                    LapRecord(
                        lapIndex = c.getInt(indexIdx),
                        checkpointMeter = c.getInt(meterIdx),
                        label = c.getString(labelIdx),
                        lapTimeMillis = c.getLong(lapTimeIdx),
                        splitTimeMillis = c.getLong(splitTimeIdx)
                    )
                )
            }
        }
        return laps
    }

    fun insertRace(race: RaceRecord) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            insertRaceInternal(db, race)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateRace(race: RaceRecord) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Update race info
            val raceValues = ContentValues().apply {
                put(COL_RACE_RUNNER_NAME, race.runnerName)
                put(COL_RACE_COMPETITION_NAME, race.competitionName)
                put(COL_RACE_DATE_STRING, race.dateString)
                put(COL_RACE_EVENT_NAME, race.eventName)
                put(COL_RACE_TOTAL_DISTANCE, race.totalDistanceMeters)
                put(COL_RACE_TOTAL_TIME, race.totalTimeMillis)
                put(COL_RACE_NOTES, race.notes)
            }
            db.update(TABLE_RACES, raceValues, "$COL_RACE_ID = ?", arrayOf(race.id))

            // Replace laps
            db.delete(TABLE_LAPS, "$COL_LAP_RACE_ID = ?", arrayOf(race.id))
            for (lap in race.laps) {
                val lapValues = ContentValues().apply {
                    put(COL_LAP_RACE_ID, race.id)
                    put(COL_LAP_INDEX, lap.lapIndex)
                    put(COL_LAP_CHECKPOINT_METER, lap.checkpointMeter)
                    put(COL_LAP_LABEL, lap.label)
                    put(COL_LAP_TIME_MILLIS, lap.lapTimeMillis)
                    put(COL_LAP_SPLIT_TIME_MILLIS, lap.splitTimeMillis)
                }
                db.insert(TABLE_LAPS, null, lapValues)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteRace(raceId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_LAPS, "$COL_LAP_RACE_ID = ?", arrayOf(raceId))
            db.delete(TABLE_RACES, "$COL_RACE_ID = ?", arrayOf(raceId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- Custom Presets ---

    fun getAllCustomPresets(): List<CheckpointPreset> {
        val presets = mutableListOf<CheckpointPreset>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CUSTOM_PRESETS, null, null, null, null, null, "$COL_PRESET_EVENT_NAME ASC")
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_PRESET_ID)
            val nameIdx = c.getColumnIndexOrThrow(COL_PRESET_EVENT_NAME)
            val distIdx = c.getColumnIndexOrThrow(COL_PRESET_TOTAL_DISTANCE)
            val csvIdx = c.getColumnIndexOrThrow(COL_PRESET_CHECKPOINTS_CSV)

            while (c.moveToNext()) {
                val checkpoints = c.getString(csvIdx)
                    .split(",")
                    .mapNotNull { it.trim().toIntOrNull() }

                presets.add(
                    CheckpointPreset(
                        id = c.getString(idIdx),
                        eventName = c.getString(nameIdx),
                        totalDistanceMeters = c.getInt(distIdx),
                        checkpoints = checkpoints,
                        isCustom = true
                    )
                )
            }
        }
        return presets
    }

    fun insertCustomPreset(preset: CheckpointPreset) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PRESET_ID, preset.id)
            put(COL_PRESET_EVENT_NAME, preset.eventName)
            put(COL_PRESET_TOTAL_DISTANCE, preset.totalDistanceMeters)
            put(COL_PRESET_CHECKPOINTS_CSV, preset.checkpoints.joinToString(","))
        }
        db.insertWithOnConflict(TABLE_CUSTOM_PRESETS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteCustomPreset(presetId: String) {
        val db = writableDatabase
        db.delete(TABLE_CUSTOM_PRESETS, "$COL_PRESET_ID = ?", arrayOf(presetId))
    }
}
