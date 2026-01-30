package iad1tya.echo.music.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.echo.innertube.YouTube
import dagger.hilt.android.qualifiers.ApplicationContext
import iad1tya.echo.music.constants.*
import iad1tya.echo.music.db.AccountDao
import iad1tya.echo.music.db.entities.AccountEntity
import iad1tya.echo.music.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    @ApplicationContext private val context: Context
) {
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val activeAccount: Flow<AccountEntity?> = accountDao.getActiveAccount()

    suspend fun getActiveAccountSync(): AccountEntity? {
        return accountDao.getActiveAccountSync()
    }

    suspend fun getAccountById(accountId: String): AccountEntity? {
        return accountDao.getAccountById(accountId)
    }

    suspend fun getAccountByEmail(email: String): AccountEntity? {
        return accountDao.getAccountByEmail(email)
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
    ): AccountEntity {
        val accountId = AccountEntity.generateAccountId()
        val account = AccountEntity(
            id = accountId,
            name = name,
            email = email,
            channelHandle = channelHandle,
            thumbnailUrl = thumbnailUrl,
            innerTubeCookie = innerTubeCookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            isActive = makeActive
        )

        if (makeActive) {
            // Deactivate all other accounts first
            accountDao.deactivateAllAccounts()
        }

        accountDao.insertAccount(account)

        if (makeActive) {
            // Update preferences and YouTube instance
            applyAccountToPreferences(account)
        }

        return account
    }

    suspend fun switchAccount(accountId: String) {
        accountDao.switchAccount(accountId)
        val account = accountDao.getAccountById(accountId)
        if (account != null) {
            applyAccountToPreferences(account)
        }
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
        if (account.isActive) {
            applyAccountToPreferences(account)
        }
    }

    suspend fun deleteAccount(accountId: String) {
        val account = accountDao.getAccountById(accountId)
        if (account?.isActive == true) {
            // If deleting active account, switch to another one or clear
            val otherAccounts = allAccounts.first().filter { it.id != accountId }
            if (otherAccounts.isNotEmpty()) {
                switchAccount(otherAccounts.first().id)
            } else {
                clearAccountPreferences()
            }
        }
        accountDao.deleteAccountById(accountId)
    }

    suspend fun getAccountCount(): Int {
        return accountDao.getAccountCount()
    }

    private suspend fun applyAccountToPreferences(account: AccountEntity) {
        context.dataStore.edit { settings ->
            settings[AccountNameKey] = account.name
            settings[AccountEmailKey] = account.email
            settings[AccountChannelHandleKey] = account.channelHandle
            settings[InnerTubeCookieKey] = account.innerTubeCookie
            settings[VisitorDataKey] = account.visitorData
            settings[DataSyncIdKey] = account.dataSyncId
            settings[ActiveAccountIdKey] = account.id
        }

        // Update YouTube instance
        YouTube.cookie = account.innerTubeCookie
        YouTube.visitorData = account.visitorData
        YouTube.dataSyncId = account.dataSyncId
    }

    private suspend fun clearAccountPreferences() {
        context.dataStore.edit { settings ->
            settings.remove(AccountNameKey)
            settings.remove(AccountEmailKey)
            settings.remove(AccountChannelHandleKey)
            settings.remove(InnerTubeCookieKey)
            settings.remove(VisitorDataKey)
            settings.remove(DataSyncIdKey)
            settings.remove(ActiveAccountIdKey)
        }

        // Clear YouTube instance
        YouTube.cookie = null
        YouTube.visitorData = null
        YouTube.dataSyncId = null
    }

    suspend fun logoutAccount(accountId: String, clearSyncedContent: Boolean = true) {
        deleteAccount(accountId)
        if (clearSyncedContent) {
            // Clear synced content - implementation depends on your sync logic
            // This should be called from the ViewModel
        }
    }
}
