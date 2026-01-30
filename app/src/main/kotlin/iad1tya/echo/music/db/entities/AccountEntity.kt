package iad1tya.echo.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey
    val id: String, // Generated unique ID for each account
    val name: String,
    val email: String,
    val channelHandle: String,
    val thumbnailUrl: String? = null,
    val innerTubeCookie: String,
    val visitorData: String,
    val dataSyncId: String,
    val isActive: Boolean = false, // Only one account can be active at a time
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUsedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun generateAccountId(): String = "ACC${System.currentTimeMillis()}"
    }
}
