package jp.co.mojaxmoja.laptracker.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import jp.co.mojaxmoja.laptracker.data.backup.GoogleDriveBackupManager
import jp.co.mojaxmoja.laptracker.data.backup.SyncStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val account by GoogleDriveBackupManager.signedInAccount.collectAsState()
    val syncStatus by GoogleDriveBackupManager.syncStatus.collectAsState()
    val lastBackupTime by GoogleDriveBackupManager.lastBackupTime.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // Google Sign-In Activity Result Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val signedInAccount = task.getResult(ApiException::class.java)
            GoogleDriveBackupManager.handleSignInResult(signedInAccount, context)
        } catch (e: Exception) {
            e.printStackTrace()
            GoogleDriveBackupManager.handleSignInResult(null, context)
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("データの復元確認", color = Color.White) },
            text = {
                Text(
                    "Google Drive上のバックアップからレース記録・走者・カスタム設定を復元します。\n既存のデータとマージされます。続行しますか？",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        coroutineScope.launch {
                            GoogleDriveBackupManager.restoreNow(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black)
                ) {
                    Text("復元を実行", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("キャンセル", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☁️ Google Drive バックアップ", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Google Driveの安全なアプリ専用バックアップ領域にレース履歴や設定を保存・引き継ぎます。",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                HorizontalDivider(color = Color(0xFF333333))

                if (account == null) {
                    // Not connected state
                    Text("Google アカウント未連携", color = Color.Gray, fontSize = 14.sp)

                    Button(
                        onClick = {
                            val client = GoogleDriveBackupManager.getSignInClient(context)
                            signInLauncher.launch(client.signInIntent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Google アカウントでログイン", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Connected state
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("連携中アカウント:", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            account?.email ?: "",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        if (lastBackupTime > 0) {
                            val timeStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(lastBackupTime))
                            Text("最終バックアップ: $timeStr", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        } else {
                            Text("バックアップ履歴: なし", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val isSyncing = syncStatus is SyncStatus.Syncing

                    // Manual Backup Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                GoogleDriveBackupManager.backupNow(context)
                            }
                        },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("今すぐバックアップ（保存）", fontWeight = FontWeight.Bold)
                    }

                    // Restore Button
                    OutlinedButton(
                        onClick = { showRestoreConfirmDialog = true },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("バックアップから復元（引き継ぎ）", fontWeight = FontWeight.Bold)
                    }

                    // Sign Out TextButton
                    TextButton(
                        onClick = {
                            GoogleDriveBackupManager.signOut(context)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("ログアウト / 連携解除", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // Sync status message
                when (syncStatus) {
                    is SyncStatus.Syncing -> {
                        Text("同期中...", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    }
                    is SyncStatus.Success -> {
                        Text((syncStatus as SyncStatus.Success).message, color = Color(0xFF00E676), fontSize = 13.sp)
                    }
                    is SyncStatus.Error -> {
                        Text((syncStatus as SyncStatus.Error).message, color = Color(0xFFFF5252), fontSize = 13.sp)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("閉じる", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
