package jp.co.mojaxmoja.laptracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.co.mojaxmoja.laptracker.data.RaceRepository
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.formatMillis
import jp.co.mojaxmoja.laptracker.data.model.formatMillisDiff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    currentRace: RaceRecord,
    onBackClick: () -> Unit
) {
    val races by RaceRepository.races.collectAsState()

    var selectedRaceA by remember(races, currentRace) {
        mutableStateOf(races.find { it.id == currentRace.id } ?: races.firstOrNull() ?: currentRace)
    }

    var selectedRaceB by remember(races, selectedRaceA) {
        val otherRace = races.firstOrNull { it.id != selectedRaceA.id } ?: selectedRaceA
        mutableStateOf(otherRace)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: 差分リスト, 1: 重ね合わせグラフ, 2: 2画面並列表示

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 レース比較分析", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBackClick) {
                Text("戻る", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Race A and Race B Selectors (ANY 2 races selection)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("任意の2つの履歴を選択して比較", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 15.sp)

                Spacer(modifier = Modifier.height(10.dp))

                // Race A Selector
                Text("🟡 比較元 (レース 1):", color = Color(0xFFFFD600), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    races.forEach { race ->
                        FilterChip(
                            selected = (selectedRaceA.id == race.id),
                            onClick = { selectedRaceA = race },
                            label = { Text("${race.runnerName} [${race.eventName}] ${race.competitionName} (${race.getFormattedTotalTime()})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD600),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Race B Selector
                Text("🔵 比較対象 (レース 2):", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    races.forEach { race ->
                        FilterChip(
                            selected = (selectedRaceB.id == race.id),
                            onClick = { selectedRaceB = race },
                            label = { Text("${race.runnerName} [${race.eventName}] ${race.competitionName} (${race.getFormattedTotalTime()})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab selection for Comparison Modes: ① 差分, ② グラフ, ③ 並列
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color(0xFFFFD600)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("① 差分表示", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("② 比較グラフ", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("③ ２画面並列", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> DiffsView(selectedRaceA, selectedRaceB)
            1 -> DualLineGraphView(selectedRaceA, selectedRaceB)
            2 -> SideBySideSplitView(selectedRaceA, selectedRaceB)
        }
    }
}

@Composable
fun DiffsView(raceA: RaceRecord, raceB: RaceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "① 各ラップタイム差分 (${raceA.runnerName}:${raceA.competitionName} vs ${raceB.runnerName}:${raceB.competitionName})",
                color = Color(0xFFFFD600),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val totalDiff = raceA.totalTimeMillis - raceB.totalTimeMillis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2B2B), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOTAL TIME 差分:", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    formatMillisDiff(totalDiff),
                    color = if (totalDiff <= 0) Color(0xFF00E5FF) else Color(0xFFFF5252),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lap diff table
            val maxLaps = maxOf(raceA.laps.size, raceB.laps.size)
            for (i in 0 until maxLaps) {
                val lapA = raceA.laps.getOrNull(i)
                val lapB = raceB.laps.getOrNull(i)

                val label = lapA?.label ?: lapB?.label ?: "Lap ${i + 1}"
                val timeA = lapA?.lapTimeMillis ?: 0L
                val timeB = lapB?.lapTimeMillis ?: 0L
                val diff = if (timeA > 0 && timeB > 0) timeA - timeB else 0L

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        if (timeA > 0) formatMillis(timeA) else "-",
                        color = Color(0xFFFFD600),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        if (timeB > 0) formatMillis(timeB) else "-",
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        if (diff != 0L) formatMillisDiff(diff) else "±0s",
                        color = if (diff <= 0) Color(0xFF00E5FF) else Color(0xFFFF5252),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f)
                    )
                }
                HorizontalDivider(color = Color(0xFF262626))
            }
        }
    }
}

@Composable
fun DualLineGraphView(raceA: RaceRecord, raceB: RaceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("② ラップ推移 重ね合わせグラフ", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFFFD600)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🟡 レース1: ${raceA.runnerName} (${raceA.competitionName})", color = Color.White, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFF00E5FF)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔵 レース2: ${raceB.runnerName} (${raceB.competitionName})", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val lapsA = raceA.laps
            val lapsB = raceB.laps
            val allTimes = lapsA.map { it.lapTimeMillis } + lapsB.map { it.lapTimeMillis }
            val minTime = (allTimes.minOrNull() ?: 30000L) - 2000L
            val maxTime = (allTimes.maxOrNull() ?: 90000L) + 2000L

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                val width = size.width
                val height = size.height

                fun getX(index: Int, total: Int): Float {
                    if (total <= 1) return width / 2
                    return (index.toFloat() / (total - 1)) * width
                }

                fun getY(time: Long): Float {
                    val range = (maxTime - minTime).toFloat()
                    if (range == 0f) return height / 2
                    val ratio = (time - minTime) / range
                    return height - (ratio * height)
                }

                // Draw Race A path (Yellow)
                if (lapsA.isNotEmpty()) {
                    val pathA = Path()
                    lapsA.forEachIndexed { i, lap ->
                        val x = getX(i, lapsA.size)
                        val y = getY(lap.lapTimeMillis)
                        if (i == 0) pathA.moveTo(x, y) else pathA.lineTo(x, y)
                        drawCircle(Color(0xFFFFD600), radius = 8f, center = Offset(x, y))
                    }
                    drawPath(pathA, Color(0xFFFFD600), style = Stroke(width = 4f))
                }

                // Draw Race B path (Cyan)
                if (lapsB.isNotEmpty()) {
                    val pathB = Path()
                    lapsB.forEachIndexed { i, lap ->
                        val x = getX(i, lapsB.size)
                        val y = getY(lap.lapTimeMillis)
                        if (i == 0) pathB.moveTo(x, y) else pathB.lineTo(x, y)
                        drawCircle(Color(0xFF00E5FF), radius = 8f, center = Offset(x, y))
                    }
                    drawPath(pathB, Color(0xFF00E5FF), style = Stroke(width = 4f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("※ 上に行くほどタイムが遅く、下に行くほど速いペースを示します", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun SideBySideSplitView(raceA: RaceRecord, raceB: RaceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("③ ２画面並列表示 (Split View)", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Column Race A
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF262626), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("🟡 " + raceA.runnerName, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(raceA.competitionName, color = Color.LightGray, fontSize = 12.sp)
                    Text("Total: ${raceA.getFormattedTotalTime()}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF444444))

                    raceA.laps.forEach { lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lap.label, color = Color.LightGray, fontSize = 11.sp)
                            Text(formatMillis(lap.lapTimeMillis), color = Color(0xFFFFD600), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Column Race B
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF262626), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("🔵 " + raceB.runnerName, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(raceB.competitionName, color = Color.LightGray, fontSize = 12.sp)
                    Text("Total: ${raceB.getFormattedTotalTime()}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF444444))

                    raceB.laps.forEach { lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lap.label, color = Color.LightGray, fontSize = 11.sp)
                            Text(formatMillis(lap.lapTimeMillis), color = Color(0xFF00E5FF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
