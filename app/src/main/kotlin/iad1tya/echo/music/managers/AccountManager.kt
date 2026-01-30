package iad1tya.echo.music.managers

import android.content.Context
import com.echo.innertube.YouTube
import dagger.hilt.android.qualifiers.ApplicationContext
import iad1tya.echo.music.db.entities.AccountEntity
import iad1tya.echo.music.repositories.AccountRepository
import iad1tya.echo.music.utils.SyncUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManager @Inject constructor(
    private val accountRepository: AccountRepository,
    private val syncUtils: SyncUtils,
    @ApplicationContext private val context: Context
) {
    val allAccounts: Flow<List<AccountEntity>> = accountRepository.allAccounts
    val activeAccount: Flow<AccountEntity?> = accountRepository.activeAccount

    suspend fun getActiveAccount(): AccountEntity? {
        return accountRepository.getActiveAccountSync()
    }

    suspend fun addAccount(
        name: String,
        email: String,
        channelHandle: String,
        thumbnailUrl: String?,
        innerTubeCookie: String,
        visitorData: String,
        dataSyncId: String,
        makeActive: Boolean = true
    ): Result<AccountEntity> {
        return try {
            // Check if account with this email already exists
            val existingAccount = accountRepository.getAccountByEmail(email)
            if (existingAccount != null) {
                // Update existing account's credentials (user is re-logging in)
                val updated = existingAccount.copy(
                    name = name,
                    channelHandle = channelHandle,
                    thumbnailUrl = thumbnailUrl,
                    innerTubeCookie = innerTubeCookie,
                    visitorData = visitorData,
                    dataSyncId = dataSyncId,
                    lastUsedAt = java.time.LocalDateTime.now()
                )
                
                if (makeActive && !existingAccount.isActive) {
                    // Switch to this account
                    accountRepository.switchAccount(existingAccount.id)
                    // Update credentials after switching
                    accountRepository.updateAccount(updated.copy(isActive = true))
                } else {
                    accountRepository.updateAccount(updated)
                }
                
                Result.success(updated)
            } else {
                // Create new account
                val account = accountRepository.addAccount(
                    name = name,
                    email = email,
                    channelHandle = channelHandle,
                    thumbnailUrl = thumbnailUrl,
                    innerTubeCookie = innerTubeCookie,
                    visitorData = visitorData,
                    dataSyncId = dataSyncId,
                    makeActive = makeActive
                )
                Result.success(account)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun switchAccount(accountId: String, clearSyncedContent: Boolean = true): Result<Unit> {
        return try {
            if (clearSyncedContent) {
                // Clear synced content before switching
                syncUtils.clearAllSyncedContent()
            }

            accountRepository.switchAccount(accountId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(accountId: String, clearSyncedContent: Boolean = true): Result<Unit> {
        return try {
            val account = accountRepository.getAccountById(accountId)
            if (account?.isActive == true && clearSyncedContent) {
                // Clear synced content if deleting active account
                syncUtils.clearAllSyncedContent()
            }

            accountRepository.deleteAccount(accountId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAccountCount(): Int {
        return accountRepository.getAccountCount()
    }

    suspend fun hasMultipleAccounts(): Boolean {
        return getAccountCount() > 1
    }

    suspend fun updateAccountThumbnail(accountId: String, thumbnailUrl: String): Result<Unit> {
        return try {
            val account = accountRepository.getAccountById(accountId)
            if (account != null) {
                accountRepository.updateAccount(account.copy(thumbnailUrl = thumbnailUrl))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
