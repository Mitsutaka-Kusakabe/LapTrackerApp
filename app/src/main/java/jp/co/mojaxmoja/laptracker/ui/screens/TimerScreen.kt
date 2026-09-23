package jp.co.mojaxmoja.laptracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.co.mojaxmoja.laptracker.data.model.CheckpointPreset
import jp.co.mojaxmoja.laptracker.data.model.LapRecord
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.formatMillis
import jp.co.mojaxmoja.laptracker.voice.VoiceLapTrigger
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(
    runnerName: String,
    competitionName: String,
    raceDate: String,
    preset: CheckpointPreset,
    isVoiceEnabled: Boolean,
    isPeakDetector: Boolean = false,
    onRaceFinished: (RaceRecord) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(false) }
    var startTimeMillis by remember { mutableStateOf(0L) }
    var elapsedTimeMillis by remember { mutableStateOf(0L) }
    var lastLapSplitTimeMillis by remember { mutableStateOf(0L) }

    val laps = remember { mutableStateListOf<LapRecord>() }
    var flashBgColor by remember { mutableStateOf(Color(0xFF121212)) }

    val nextCheckpointIndex = laps.size
    val nextCheckpointMeter = if (nextCheckpointIndex < preset.checkpoints.size) {
        preset.checkpoints[nextCheckpointIndex]
    } else {
        preset.totalDistanceMeters
    }

    val animatedBgColor by animateColorAsState(targetValue = flashBgColor, label = "bg")

    // Timer loop with millisecond precision
    LaunchedEffect(isRunning, startTimeMillis) {
        if (isRunning) {
            while (isRunning) {
                elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
                kotlinx.coroutines.android.awaitFrame()
            }
        }
    }

    val recordLapAction: () -> Unit = {
        if (isRunning) {
            val currentSplit = elapsedTimeMillis
            val lapTime = currentSplit - lastLapSplitTimeMillis
            lastLapSplitTimeMillis = currentSplit

            val checkpointIndex = laps.size
            val checkpointMeter = if (checkpointIndex < preset.checkpoints.size) {
                preset.checkpoints[checkpointIndex]
            } else {
                preset.totalDistanceMeters
            }

            val newLap = LapRecord(
                lapIndex = laps.size + 1,
                checkpointMeter = checkpointMeter,
                label = "${checkpointMeter}m",
                lapTimeMillis = lapTime,
                splitTimeMillis = currentSplit
            )
            laps.add(0, newLap) // Add at top for current view

            // Visual flash on lap
            flashBgColor = Color(0xFF004D40)
        }
    }

    LaunchedEffect(flashBgColor) {
        if (flashBgColor != Color(0xFF121212)) {
            delay(150)
            flashBgColor = Color(0xFF121212)
        }
    }

    // Setup voice recognizer
    DisposableEffect(isVoiceEnabled, isPeakDetector, isRunning) {
        var voiceTrigger: VoiceLapTrigger? = null
        if (isVoiceEnabled && isRunning) {
            voiceTrigger = VoiceLapTrigger(context, usePeakDetector = isPeakDetector) { keyword ->
                recordLapAction()
            }.apply {
                startListening()
            }
        }
        onDispose {
            voiceTrigger?.stopListening()
        }
    }

    val currentLapTime = elapsedTimeMillis - lastLapSplitTimeMillis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(runnerName, color = Color(0xFFFFD600), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${preset.eventName} • $competitionName", color = Color.Gray, fontSize = 13.sp)
            }
            if (isVoiceEnabled) {
                Surface(
                    color = if (isRunning) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.DarkGray,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "🗣️ 音声トリガーON",
                        color = if (isRunning) Color(0xFF00E5FF) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Main Digital Clock Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    if (!isRunning) {
                        isRunning = true
                        startTimeMillis = System.currentTimeMillis()
                        lastLapSplitTimeMillis = 0L
                    } else {
                        recordLapAction()
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (!isRunning && elapsedTimeMillis == 0L) "画面タップでスタート" else "トータル経過時間",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Text(
                    text = formatMillis(elapsedTimeMillis),
                    color = Color(0xFFFFD600),
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("カレントラップ", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            formatMillis(currentLapTime),
                            color = Color(0xFF00E5FF),
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("次通過地点", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            "${nextCheckpointMeter}m",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFF333333),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        if (!isRunning) "▶ タップして開始" else "👆 タップ / 発声「400」でラップ記録",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Lap List
        Text("Recorded Laps (${laps.size})", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            items(laps) { lap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap ${lap.lapIndex} (${lap.label})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("ラップ: ${formatMillis(lap.lapTimeMillis)}", color = Color(0xFF00E5FF), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("通過: ${formatMillis(lap.splitTimeMillis)}", color = Color.LightGray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }
                HorizontalDivider(color = Color(0xFF222222))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("中止")
            }

            Button(
                onClick = {
                    isRunning = false
                    if (laps.isEmpty() || laps.first().splitTimeMillis < elapsedTimeMillis) {
                        val currentSplit = elapsedTimeMillis
                        val lapTime = currentSplit - lastLapSplitTimeMillis
                        val finalLap = LapRecord(
                            lapIndex = laps.size + 1,
                            checkpointMeter = preset.totalDistanceMeters,
                            label = "${preset.totalDistanceMeters}m (Finish)",
                            lapTimeMillis = lapTime,
                            splitTimeMillis = currentSplit
                        )
                        laps.add(0, finalLap)
                    }

                    val raceRecord = RaceRecord(
                        runnerName = runnerName,
                        competitionName = competitionName,
                        dateString = raceDate,
                        eventName = preset.eventName,
                        totalDistanceMeters = preset.totalDistanceMeters,
                        totalTimeMillis = elapsedTimeMillis,
                        laps = laps.reversed()
                    )
                    onRaceFinished(raceRecord)
                },
                enabled = elapsedTimeMillis > 0,
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
            ) {
                Text("🏁 フィニッシュ・保存", fontWeight = FontWeight.Bold)
            }
        }
    }
}
