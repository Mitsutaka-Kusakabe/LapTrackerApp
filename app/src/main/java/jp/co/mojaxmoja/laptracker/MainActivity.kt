package jp.co.mojaxmoja.laptracker

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import jp.co.mojaxmoja.laptracker.data.RaceRepository
import jp.co.mojaxmoja.laptracker.data.model.CheckpointPreset
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.theme.LapTrackerTheme
import jp.co.mojaxmoja.laptracker.ui.screens.*

enum class AppScreen {
    SETUP,
    TIMER,
    RESULT_SHARE,
    COMPARE,
    HISTORY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RaceRepository.init(this)
        enableEdgeToEdge()

        setContent {
            LapTrackerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                ) {
                    LapTrackerApp()
                }
            }
        }
    }
}

@Composable
fun LapTrackerApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.SETUP) }

    // Race Session State
    var activeRunnerName by remember { mutableStateOf("") }
    var activeCompetitionName by remember { mutableStateOf("") }
    var activeRaceDate by remember { mutableStateOf("") }
    var activePreset by remember { mutableStateOf(CheckpointPreset.DEFAULT_PRESETS[1]) }
    var activeVoiceEnabled by remember { mutableStateOf(true) }
    var activePeakDetector by remember { mutableStateOf(false) }

    var currentRaceRecord by remember { mutableStateOf<RaceRecord?>(null) }

    // Request Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Permission handled */ }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    when (currentScreen) {
        AppScreen.SETUP -> {
            RaceSetupScreen(
                onStartTimer = { runner, competition, date, preset, isVoice, isPeak ->
                    activeRunnerName = runner
                    activeCompetitionName = competition
                    activeRaceDate = date
                    activePreset = preset
                    activeVoiceEnabled = isVoice
                    activePeakDetector = isPeak
                    currentScreen = AppScreen.TIMER
                },
                onNavigateHistory = {
                    currentScreen = AppScreen.HISTORY
                }
            )
        }

        AppScreen.TIMER -> {
            TimerScreen(
                runnerName = activeRunnerName,
                competitionName = activeCompetitionName,
                raceDate = activeRaceDate,
                preset = activePreset,
                isVoiceEnabled = activeVoiceEnabled,
                isPeakDetector = activePeakDetector,
                onRaceFinished = { raceRecord ->
                    RaceRepository.saveRace(raceRecord)
                    currentRaceRecord = raceRecord
                    currentScreen = AppScreen.RESULT_SHARE
                },
                onCancel = {
                    currentScreen = AppScreen.SETUP
                }
            )
        }

        AppScreen.RESULT_SHARE -> {
            val race = currentRaceRecord ?: RaceRepository.races.value.first()
            ResultShareScreen(
                race = race,
                onCompareClick = {
                    currentScreen = AppScreen.COMPARE
                },
                onHomeClick = {
                    currentScreen = AppScreen.SETUP
                }
            )
        }

        AppScreen.COMPARE -> {
            val race = currentRaceRecord ?: RaceRepository.races.value.first()
            CompareScreen(
                currentRace = race,
                onBackClick = {
                    currentScreen = AppScreen.RESULT_SHARE
                }
            )
        }

        AppScreen.HISTORY -> {
            HistoryListScreen(
                onRaceSelected = { selectedRace ->
                    currentRaceRecord = selectedRace
                    currentScreen = AppScreen.RESULT_SHARE
                },
                onCompareTwoRaces = { race1, race2 ->
                    currentRaceRecord = race1
                    currentScreen = AppScreen.COMPARE
                },
                onBackClick = {
                    currentScreen = AppScreen.SETUP
                }
            )
        }
    }
}
