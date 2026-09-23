package com.example.laptracker.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laptracker.data.RaceRepository
import com.example.laptracker.data.model.RaceRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryListScreen(
    onRaceSelected: (RaceRecord) -> Unit,
    onCompareTwoRaces: (RaceRecord, RaceRecord) -> Unit,
    onBackClick: () -> Unit
) {
    val races by RaceRepository.races.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var raceToDelete by remember { mutableStateOf<RaceRecord?>(null) }

    val selectedCompareIds = remember { mutableStateListOf<String>() }

    val filteredRaces = remember(races, searchQuery) {
        if (searchQuery.isBlank()) races
        else races.filter {
            it.runnerName.contains(searchQuery, ignoreCase = true) ||
                    it.competitionName.contains(searchQuery, ignoreCase = true) ||
                    it.eventName.contains(searchQuery, ignoreCase = true)
        }
    }

    if (raceToDelete != null) {
        AlertDialog(
            onDismissRequest = { raceToDelete = null },
            title = { Text("履歴の削除", color = Color.White) },
            text = { Text("${raceToDelete?.runnerName} - ${raceToDelete?.competitionName} の記録を削除しますか？", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        raceToDelete?.let { RaceRepository.deleteRace(it.id) }
                        raceToDelete = null
                    }
                ) {
                    Text("削除", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { raceToDelete = null }) {
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
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📜 ラップ計測履歴", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBackClick) {
                Text("戻る", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("選手名・大会名・種目で検索...", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD600)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "※ チェックボックスで2件選択して直接比較できます",
            color = Color(0xFF00E5FF),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (filteredRaces.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("保存された履歴がありません", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredRaces, key = { it.id }) { race ->
                        val isChecked = selectedCompareIds.contains(race.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRaceSelected(race) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) Color(0xFF2A2A00) else Color(0xFF1E1E1E)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (selectedCompareIds.size >= 2) {
                                                selectedCompareIds.removeAt(0) // Keep max 2
                                            }
                                            selectedCompareIds.add(race.id)
                                        } else {
                                            selectedCompareIds.remove(race.id)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD600))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            race.runnerName,
                                            color = Color(0xFFFFD600),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                race.eventName,
                                                color = Color(0xFF00E5FF),
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(race.competitionName, color = Color.LightGray, fontSize = 13.sp)
                                    Text(race.dateString, color = Color.Gray, fontSize = 11.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            race.getFormattedTotalTime(),
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text("詳細 ➔", color = Color(0xFFFFD600), fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { raceToDelete = race }
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedCompareIds.size == 2) {
            Spacer(modifier = Modifier.height(12.dp))
            val race1 = races.find { it.id == selectedCompareIds[0] }
            val race2 = races.find { it.id == selectedCompareIds[1] }
            if (race1 != null && race2 != null) {
                Button(
                    onClick = { onCompareTwoRaces(race1, race2) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("📊 選択した2件を比較分析する ➔", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
