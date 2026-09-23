package com.example.laptracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laptracker.data.model.CheckpointPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceSetupScreen(
    onStartTimer: (runnerName: String, competitionName: String, raceDate: String, preset: CheckpointPreset, isVoiceEnabled: Boolean, isPeakDetector: Boolean) -> Unit,
    onNavigateHistory: () -> Unit
) {
    val presets by CheckpointPreset.allPresets.collectAsState()

    var runnerName by remember { mutableStateOf("山田 太郎") }
    var competitionName by remember { mutableStateOf("第1回 競技会") }
    var raceDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
    }
    var selectedPreset by remember(presets) { mutableStateOf(presets.firstOrNull { it.eventName == "1500m" } ?: presets.first()) }
    var isVoiceEnabled by remember { mutableStateOf(true) }
    var isPeakDetector by remember { mutableStateOf(false) } // False: Keyword mode, True: 0ms Sound Peak mode

    // Custom Preset Creation Dialog State
    var showAddPresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newCheckpointsStr by remember { mutableStateOf("400, 800, 1200, 1600, 2000") }

    if (showAddPresetDialog) {
        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            title = { Text("＋ 新規カスタムラップセット作成", color = Color.White) },
            text = {
                Column {
                    Text("任意の種目名と通過地点（メートル）を設定できます", color = Color.Gray, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("種目名 (例: 2000mSC, 10000m, 400m×5本)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newCheckpointsStr,
                        onValueChange = { newCheckpointsStr = it },
                        label = { Text("通過地点メートル (カンマ区切り)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val points = newCheckpointsStr
                            .split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .filter { it > 0 }
                        if (newPresetName.isNotBlank() && points.isNotEmpty()) {
                            val created = CheckpointPreset.addCustomPreset(newPresetName, points)
                            selectedPreset = created
                            showAddPresetDialog = false
                            newPresetName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black)
                ) {
                    Text("保存・使用", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPresetDialog = false }) {
                    Text("キャンセル", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏃 陸上ラップタイマー",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onNavigateHistory,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
            ) {
                Text("履歴・比較")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("レース情報入力", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = runnerName,
                    onValueChange = { runnerName = it },
                    label = { Text("選手名 / 人", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD600)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = competitionName,
                    onValueChange = { competitionName = it },
                    label = { Text("大会名 / 競技会名", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD600)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = raceDate,
                    onValueChange = { raceDate = it },
                    label = { Text("計測日時", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD600)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Checkpoint Presets Selection & Creation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("種目・ラップ地点プリセット", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Button(
                        onClick = { showAddPresetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("＋ カスタム作成", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = (selectedPreset.id == preset.id),
                            onClick = { selectedPreset = preset },
                            label = {
                                Text(
                                    text = if (preset.isCustom) "⭐ ${preset.eventName}" else preset.eventName,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD600),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "📍 チェックポイント通過地点 (${selectedPreset.eventName}):",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Text(
                            selectedPreset.checkpoints.joinToString("m ➔ ") + "m (Finish)",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🗣️ 自動ラップトリガー設定", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("発声または大声・ホイッスルでタップレス計測", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isVoiceEnabled,
                        onCheckedChange = { isVoiceEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD600))
                    )
                }

                if (isVoiceEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("検知モード選択:", color = Color.Gray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isPeakDetector,
                            onClick = { isPeakDetector = false },
                            label = { Text("🗣️ 単語音声認識 (400,はい等)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD600),
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = isPeakDetector,
                            onClick = { isPeakDetector = true },
                            label = { Text("⚡ 超高速音量ピーク (0msラグ)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (runnerName.isNotBlank()) {
                    onStartTimer(runnerName, competitionName, raceDate, selectedPreset, isVoiceEnabled, isPeakDetector)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("⏱️ 計測スタート", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
