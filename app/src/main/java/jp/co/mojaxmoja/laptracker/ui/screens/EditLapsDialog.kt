package jp.co.mojaxmoja.laptracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jp.co.mojaxmoja.laptracker.data.model.LapRecord
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.formatMillis
import jp.co.mojaxmoja.laptracker.data.model.recalculateLaps

@Composable
fun EditLapsDialog(
    race: RaceRecord,
    onSave: (RaceRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var editableLaps by remember { mutableStateOf(race.laps) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "✏️ ラップ修正・編集",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${race.runnerName} - ${race.eventName}",
                            color = Color(0xFFFFD600),
                            fontSize = 14.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = Color.Gray, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "※ 不要な誤タップは「🗑️」で削除できます。前後の区間ラップタイムは自動で正しく再計算されます。",
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lap", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(0.6f))
                    Text("距離(m)", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                    Text("区間ラップ", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                    Text("通算通過", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                    Text("削除", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Laps List
                Box(modifier = Modifier.weight(1f)) {
                    if (editableLaps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("ラップがありません", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(editableLaps) { index, lap ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF262626), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Lap index
                                    Text(
                                        "${lap.lapIndex}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(0.6f)
                                    )

                                    // Distance (checkpointMeter) editable field
                                    OutlinedTextField(
                                        value = if (lap.checkpointMeter > 0) lap.checkpointMeter.toString() else "",
                                        onValueChange = { newVal ->
                                            val parsed = newVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                                            editableLaps = editableLaps.toMutableList().also { list ->
                                                list[index] = lap.copy(
                                                    checkpointMeter = parsed,
                                                    label = "${parsed}m"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFFFD600),
                                            unfocusedBorderColor = Color.DarkGray
                                        ),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .padding(end = 4.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    )

                                    // Lap Time
                                    Text(
                                        formatMillis(lap.lapTimeMillis),
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    // Split Time
                                    Text(
                                        formatMillis(lap.splitTimeMillis),
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    // Delete Action
                                    IconButton(
                                        onClick = {
                                            val remaining = editableLaps.filterIndexed { i, _ -> i != index }
                                            editableLaps = recalculateLaps(remaining)
                                        },
                                        modifier = Modifier.weight(0.7f)
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("キャンセル")
                    }

                    Button(
                        onClick = {
                            val recalculated = recalculateLaps(editableLaps)
                            val finalTotalTime = recalculated.lastOrNull()?.splitTimeMillis ?: race.totalTimeMillis
                            val finalTotalDistance = recalculated.lastOrNull()?.checkpointMeter ?: race.totalDistanceMeters
                            val updated = race.copy(
                                laps = recalculated,
                                totalTimeMillis = finalTotalTime,
                                totalDistanceMeters = finalTotalDistance
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black)
                    ) {
                        Text("修正を保存する", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
