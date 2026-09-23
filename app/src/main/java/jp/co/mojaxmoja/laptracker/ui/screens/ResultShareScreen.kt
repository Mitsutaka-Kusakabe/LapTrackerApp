package jp.co.mojaxmoja.laptracker.ui.screens

import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.co.mojaxmoja.laptracker.data.model.RaceRecord
import jp.co.mojaxmoja.laptracker.data.model.formatMillis
import jp.co.mojaxmoja.laptracker.share.ImageShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultShareScreen(
    race: RaceRecord,
    onCompareClick: (RaceRecord) -> Unit,
    onHomeClick: () -> Unit
) {
    val context = LocalContext.current
    val picture = remember { Picture() }

    fun captureBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.parseColor("#121212"))
        picture.draw(canvas)
        return bitmap
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
            Text("🏁 計測結果", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = onHomeClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("ホーム")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Single-Screen Screenshot Optimized Card View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val width = size.width.toInt()
                    val height = size.height.toInt()
                    onDrawWithContent {
                        val pictureCanvas = androidx.compose.ui.graphics.Canvas(
                            picture.beginRecording(width, height)
                        )
                        draw(this, layoutDirection, pictureCanvas, size) {
                            this@onDrawWithContent.drawContent()
                        }
                        picture.endRecording()
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawPicture(picture)
                        }
                    }
                }
                .border(2.dp, Color(0xFFFFD600), RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header Meta Data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = race.runnerName,
                            color = Color(0xFFFFD600),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${race.eventName} (${race.totalDistanceMeters}m)",
                            color = Color(0xFF00E5FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(race.competitionName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(race.dateString, color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(16.dp))

                // Total Time
                Text("TOTAL TIME", color = Color.Gray, fontSize = 12.sp, letterSpacing = 2.sp)
                Text(
                    text = race.getFormattedTotalTime(),
                    color = Color(0xFFFFD600),
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(12.dp))

                // Vertical Lap Table (Optimized for 1-Screen Screenshot)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("区間", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("ラップタイム", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                    Text("通過スプリット", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                }

                Spacer(modifier = Modifier.height(4.dp))

                race.laps.forEach { lap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lap.label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatMillis(lap.lapTimeMillis),
                            color = Color(0xFF00E5FF),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = formatMillis(lap.splitTimeMillis),
                            color = Color.LightGray,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Divider(color = Color(0xFF262626))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Generated by LapTracker 🏃", color = Color.DarkGray, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = {
                val bitmap = captureBitmap()
                ImageShareUtil.shareBitmap(context, bitmap, "${race.runnerName} - ${race.eventName}")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("📲 LINE / SNSに1画面共有", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onCompareClick(race) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("📊 過去のデータと比較分析", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
