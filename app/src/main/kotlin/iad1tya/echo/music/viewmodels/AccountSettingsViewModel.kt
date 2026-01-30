package iad1tya.echo.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.App
import iad1tya.echo.music.managers.AccountManager
import iad1tya.echo.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val syncUtils: SyncUtils,
    private val accountManager: AccountManager,
) : ViewModel() {

    val allAccounts = accountManager.allAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val activeAccount = accountManager.activeAccount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Switch to a different account
     */
    fun switchAccount(accountId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            accountManager.switchAccount(accountId, clearSyncedContent = true)
        }
    }

    /**
     * Logout user and clear all synced content to prevent data mixing between accounts
     */
    fun logoutAccount(context: Context, accountId: String, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            accountManager.deleteAccount(accountId, clearSyncedContent = true)
            
            // Clear cookie in UI if this was the active account
            if (accountManager.getActiveAccount() == null) {
                onCookieChange("")
            }
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    fun logoutAndClearSyncedContent(context: Context, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all YouTube Music synced content first
            syncUtils.clearAllSyncedContent()
            
            // Then clear account preferences
            App.forgetAccount(context)
            
            // Clear cookie in UI
            onCookieChange("")
        }
    }
}