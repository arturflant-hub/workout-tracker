package com.workouttracker.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.workouttracker.BuildConfig
import com.workouttracker.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupFileInfo(
    val id: String,
    val name: String,
    val date: String
)

@Singleton
class GoogleDriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val DB_NAME = "workout_tracker.db"
        private const val PREFS_NAME = "workout_prefs"
        private const val BACKUP_PREFIX = "workout_tracker_backup_"
        private const val METADATA_FILE = "backup_metadata.json"
    }

    private val gsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    private fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = getGoogleSignInClient().signInIntent

    fun signOut() {
        getGoogleSignInClient().signOut()
    }

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.result
        } catch (e: Exception) {
            null
        }
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(httpTransport, gsonFactory, credential)
            .setApplicationName("WorkoutTracker")
            .build()
    }

    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(Exception("Не авторизован в Google"))

            // WAL checkpoint — flush all WAL data to main DB file
            val cursor = database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
            cursor.close()

            val tempDir = File(context.cacheDir, "backup_temp").apply {
                deleteRecursively()
                mkdirs()
            }

            try {
                // Copy DB file + WAL/SHM for completeness
                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists()) {
                    dbFile.copyTo(File(tempDir, DB_NAME), overwrite = true)
                }
                val walFile = File(dbFile.path + "-wal")
                if (walFile.exists()) {
                    walFile.copyTo(File(tempDir, "$DB_NAME-wal"), overwrite = true)
                }
                val shmFile = File(dbFile.path + "-shm")
                if (shmFile.exists()) {
                    shmFile.copyTo(File(tempDir, "$DB_NAME-shm"), overwrite = true)
                }

                // Copy SharedPreferences
                val prefsFile = File(context.filesDir.parent, "shared_prefs/${PREFS_NAME}.xml")
                if (prefsFile.exists()) {
                    prefsFile.copyTo(File(tempDir, "${PREFS_NAME}.xml"), overwrite = true)
                }

                // Create metadata
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale("ru"))
                val metadata = JSONObject().apply {
                    put("backupDate", sdf.format(Date()))
                    put("appVersionName", BuildConfig.VERSION_NAME)
                    put("appVersionCode", BuildConfig.VERSION_CODE)
                    put("dbVersion", 4)
                }
                File(tempDir, METADATA_FILE).writeText(metadata.toString(2))

                // Create ZIP
                val zipSdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("ru"))
                val zipName = "${BACKUP_PREFIX}${zipSdf.format(Date())}.zip"
                val zipFile = File(context.cacheDir, zipName)

                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    tempDir.listFiles()?.forEach { file ->
                        zos.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // Upload to Google Drive
                val driveService = getDriveService(account)
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = zipName
                    mimeType = "application/zip"
                }
                val content = InputStreamContent("application/zip", FileInputStream(zipFile))
                content.length = zipFile.length()

                driveService.files().create(fileMetadata, content)
                    .setFields("id, name")
                    .execute()

                // Cleanup
                zipFile.delete()
                tempDir.deleteRecursively()

                Result.success(zipName)
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBackups(): Result<List<BackupFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(Exception("Не авторизован в Google"))

            val driveService = getDriveService(account)
            val result = driveService.files().list()
                .setQ("name contains '$BACKUP_PREFIX' and mimeType = 'application/zip' and trashed = false")
                .setOrderBy("modifiedTime desc")
                .setPageSize(20)
                .setFields("files(id, name, modifiedTime)")
                .execute()

            val files = result.files ?: emptyList()
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
            val backups = files.map { file ->
                val date = try {
                    sdf.format(Date(file.modifiedTime.value))
                } catch (e: Exception) {
                    "неизвестно"
                }
                BackupFileInfo(id = file.id, name = file.name, date = date)
            }
            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(fileId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(Exception("Не авторизован в Google"))

            val driveService = getDriveService(account)

            // Download
            val zipFile = File(context.cacheDir, "restore_temp.zip")
            FileOutputStream(zipFile).use { fos ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(fos)
            }

            // Extract
            val extractDir = File(context.cacheDir, "restore_extract").apply {
                deleteRecursively()
                mkdirs()
            }

            try {
                ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(extractDir, entry.name)
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                // Read metadata
                val metadataFile = File(extractDir, METADATA_FILE)
                val backupDate = if (metadataFile.exists()) {
                    val json = JSONObject(metadataFile.readText())
                    json.optString("backupDate", "неизвестно")
                } else "неизвестно"

                // Close database before replacing
                database.close()

                // Replace DB
                val extractedDb = File(extractDir, DB_NAME)
                if (extractedDb.exists()) {
                    val targetDb = context.getDatabasePath(DB_NAME)
                    // Delete WAL and SHM files first
                    File(targetDb.path + "-wal").delete()
                    File(targetDb.path + "-shm").delete()
                    extractedDb.copyTo(targetDb, overwrite = true)

                    // Restore WAL/SHM if they were in backup
                    val extractedWal = File(extractDir, "$DB_NAME-wal")
                    if (extractedWal.exists()) {
                        extractedWal.copyTo(File(targetDb.path + "-wal"), overwrite = true)
                    }
                    val extractedShm = File(extractDir, "$DB_NAME-shm")
                    if (extractedShm.exists()) {
                        extractedShm.copyTo(File(targetDb.path + "-shm"), overwrite = true)
                    }
                }

                // Replace SharedPreferences
                val extractedPrefs = File(extractDir, "${PREFS_NAME}.xml")
                if (extractedPrefs.exists()) {
                    val targetPrefs = File(context.filesDir.parent, "shared_prefs/${PREFS_NAME}.xml")
                    extractedPrefs.copyTo(targetPrefs, overwrite = true)
                }

                // Cleanup
                zipFile.delete()
                extractDir.deleteRecursively()

                Result.success(backupDate)
            } finally {
                extractDir.deleteRecursively()
                zipFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(Exception("Не авторизован в Google"))

            val driveService = getDriveService(account)
            driveService.files().delete(fileId).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}

class BackupNotFoundException : Exception("Резервная копия не найдена")
