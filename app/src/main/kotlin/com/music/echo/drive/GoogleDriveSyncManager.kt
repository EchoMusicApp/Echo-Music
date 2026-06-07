package iad1tya.echo.music.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.client.http.FileContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import timber.log.Timber

object GoogleDriveSyncManager {

    private const val BACKUP_FILE_NAME = "EchoMusicBackup.zip"

    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            return account
        }
        return null
    }

    fun signOut(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
    }

    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Echo Music")
            .build()
    }

    suspend fun uploadBackupZip(context: Context, backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Not signed in"))

            val driveService = getDriveService(context, account)

            // Check if file already exists
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id, name)")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }

            val mediaContent = FileContent("application/zip", backupFile)

            if (existingFileId != null) {
                // Update existing file
                val fileUpdateMetadata = com.google.api.services.drive.model.File().apply {
                    name = BACKUP_FILE_NAME
                }
                driveService.files().update(existingFileId, fileUpdateMetadata, mediaContent).execute()
            } else {
                // Create new file
                driveService.files().create(fileMetadata, mediaContent).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Drive Upload Failed")
            Result.failure(e)
        }
    }

    suspend fun downloadBackupZip(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Not signed in"))

            val driveService = getDriveService(context, account)

            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            val existingFile = fileList.files.firstOrNull()
                ?: return@withContext Result.failure(Exception("No backup found on Drive"))

            val destFile = File(context.cacheDir, "downloaded_$BACKUP_FILE_NAME")
            FileOutputStream(destFile).use { outStream ->
                driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outStream)
            }

            Result.success(destFile)
        } catch (e: Exception) {
            Timber.e(e, "Drive Download Failed")
            Result.failure(e)
        }
    }

    suspend fun getBackupTime(context: Context): Long? = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount(context) ?: return@withContext null
            val driveService = getDriveService(context, account)
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id, modifiedTime)")
                .execute()
            
            val existingFile = fileList.files.firstOrNull()
            existingFile?.modifiedTime?.value
        } catch (e: Exception) {
            null
        }
    }
}
