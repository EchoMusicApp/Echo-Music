package iad1tya.echo.music

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper
import android.os.ParcelFileDescriptor
import iad1tya.echo.music.db.InternalDatabase
import iad1tya.echo.music.viewmodels.BackupRestoreViewModel.Companion.SETTINGS_FILENAME
import iad1tya.echo.music.utils.dataStore
import androidx.datastore.preferences.core.edit
import timber.log.Timber
import kotlinx.coroutines.flow.first

class EchoBackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        super.onCreate()
        
        // Settings backup
        SharedPreferencesBackupHelper(this, packageName + "_preferences").also {
            addHelper("prefs", it)
        }
        
        // Datastore backup
        FileBackupHelper(this, "../files/datastore/$SETTINGS_FILENAME").also {
            addHelper("datastore", it)
        }
        
        // Database backup
        FileBackupHelper(
            this, 
            "../databases/${InternalDatabase.DB_NAME}",
            "../databases/${InternalDatabase.DB_NAME}-wal",
            "../databases/${InternalDatabase.DB_NAME}-shm"
        ).also {
            addHelper("database", it)
        }
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        val isEnabled = kotlinx.coroutines.runBlocking {
            this@EchoBackupAgent.dataStore.data.first()[iad1tya.echo.music.constants.EnableCloudBackupKey] ?: true
        }
        if (!isEnabled) {
            Timber.tag("EchoBackupAgent").i("Cloud backup is disabled by user. Skipping.")
            return
        }

        Timber.tag("EchoBackupAgent").i("Preparing for cloud backup. Forcing WAL checkpoint.")
        try {
            // Force checkpoint to flush WAL before system copies the files
            val db = InternalDatabase.newInstance(this)
            db.query {
                openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            }
            db.close()
            Timber.tag("EchoBackupAgent").i("WAL checkpoint completed successfully.")
            
            // Record backup timestamp
            kotlinx.coroutines.runBlocking {
                this@EchoBackupAgent.dataStore.edit { prefs ->
                    prefs[iad1tya.echo.music.constants.LastCloudBackupTimeKey] = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            Timber.tag("EchoBackupAgent").e(e, "Failed to checkpoint database before backup")
        }

        super.onBackup(oldState, data, newState)
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        Timber.tag("EchoBackupAgent").i("Starting cloud restore...")
        super.onRestore(data, appVersionCode, newState)
        Timber.tag("EchoBackupAgent").i("Cloud restore completed.")
    }
}
