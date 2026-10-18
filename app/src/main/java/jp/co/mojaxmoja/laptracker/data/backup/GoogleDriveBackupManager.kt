package jp.co.mojaxmoja.laptracker.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import jp.co.mojaxmoja.laptracker.data.RaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.util.Collections

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

object GoogleDriveBackupManager {
    private const val TAG = "GoogleDriveBackup"
    private const val BACKUP_FILE_NAME = "laptracker_backup.json"
    private const val PREFS_NAME = "laptracker_drive_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"

    private val _signedInAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val signedInAccount: StateFlow<GoogleSignInAccount?> = _signedInAccount.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long>(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    fun init(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val requiredScope = Scope(DriveScopes.DRIVE_APPDATA)
        if (account != null && GoogleSignIn.hasPermissions(account, requiredScope)) {
            _signedInAccount.value = account
        } else {
            _signedInAccount.value = null
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _lastBackupTime.value = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleSignInResult(account: GoogleSignInAccount?, context: Context) {
        _signedInAccount.value = account
        if (account != null) {
            // Once signed in, trigger an automatic check/sync in background
            _syncStatus.value = SyncStatus.Success("Google Driveに接続しました: ${account.email}")
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        val client = getSignInClient(context)
        client.signOut().addOnCompleteListener {
            _signedInAccount.value = null
            _syncStatus.value = SyncStatus.Idle
            onComplete()
        }
    }

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("LapTracker").build()

    }

    suspend fun backupNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val account = _signedInAccount.value
            ?: return@withContext Result.failure(IllegalStateException("Googleアカウントにサインインしていません"))

        _syncStatus.value = SyncStatus.Syncing
        try {
            val driveService = getDriveService(context, account)
            val payload = RaceRepository.exportBackupPayload()
            val jsonContent = payload.toJson()
            val contentBytes = jsonContent.toByteArray(Charsets.UTF_8)
            val mediaContent = ByteArrayContent("application/json", contentBytes)

            // Search existing backup file in appDataFolder
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            val existingFile = fileList.files?.firstOrNull()
            if (existingFile != null) {
                // Update existing file
                val fileMetadata = File()
                driveService.files().update(existingFile.id, fileMetadata, mediaContent).execute()
                Log.d(TAG, "Updated existing backup file ID: ${existingFile.id}")
            } else {
                // Create new file in appDataFolder
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                val created = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                Log.d(TAG, "Created new backup file ID: ${created.id}")
            }

            val now = System.currentTimeMillis()
            _lastBackupTime.value = now
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_BACKUP_TIME, now).apply()

            val successMsg = "バックアップが完了しました (${payload.races.size}件の記録)"
            _syncStatus.value = SyncStatus.Success(successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            val errMsg = "バックアップ失敗: ${e.localizedMessage ?: e.message}"
            _syncStatus.value = SyncStatus.Error(errMsg)
            Result.failure(e)
        }
    }

    suspend fun restoreNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val account = _signedInAccount.value
            ?: return@withContext Result.failure(IllegalStateException("Googleアカウントにサインインしていません"))

        _syncStatus.value = SyncStatus.Syncing
        try {
            val driveService = getDriveService(context, account)

            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            val existingFile = fileList.files?.firstOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Google Drive上にバックアップデータが見つかりませんでした"))

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
            val jsonContent = outputStream.toString("UTF-8")

            val payload = BackupPayload.fromJson(jsonContent)
            withContext(Dispatchers.Main) {
                RaceRepository.restoreBackupPayload(payload)
            }

            val successMsg = "復元が完了しました (記録: ${payload.races.size}件, 走者: ${payload.runners.size}名)"
            _syncStatus.value = SyncStatus.Success(successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            val errMsg = "復元失敗: ${e.localizedMessage ?: e.message}"
            _syncStatus.value = SyncStatus.Error(errMsg)
            Result.failure(e)
        }
    }

    // Auto backup triggered on race save
    fun autoBackupInBackground(context: Context) {
        if (_signedInAccount.value == null) return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                backupNow(context)
            } catch (e: Exception) {
                Log.w(TAG, "Auto-backup failed silently", e)
            }
        }
    }
}
