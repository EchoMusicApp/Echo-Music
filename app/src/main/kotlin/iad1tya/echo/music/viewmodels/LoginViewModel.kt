package iad1tya.echo.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iad1tya.echo.music.db.entities.AccountEntity
import iad1tya.echo.music.managers.AccountManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {

    suspend fun addAccount(
        name: String,
        email: String,
        channelHandle: String,
        thumbnailUrl: String?,
        innerTubeCookie: String,
        visitorData: String,
        dataSyncId: String
    ): Result<AccountEntity> {
        return accountManager.addAccount(
            name = name,
            email = email,
            channelHandle = channelHandle,
            thumbnailUrl = thumbnailUrl,
            innerTubeCookie = innerTubeCookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            makeActive = true
        )
    }
}
