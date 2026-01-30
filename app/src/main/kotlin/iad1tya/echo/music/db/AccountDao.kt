package iad1tya.echo.music.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import iad1tya.echo.music.db.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM account ORDER BY lastUsedAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<AccountEntity?>

    @Query("SELECT * FROM account WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccountSync(): AccountEntity?

    @Query("SELECT * FROM account WHERE id = :accountId")
    suspend fun getAccountById(accountId: String): AccountEntity?

    @Query("SELECT * FROM account WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM account WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Transaction
    suspend fun switchAccount(newAccountId: String) {
        // Deactivate all accounts
        deactivateAllAccounts()
        
        // Activate the selected account and update its last used time
        val account = getAccountById(newAccountId)
        if (account != null) {
            updateAccount(
                account.copy(
                    isActive = true,
                    lastUsedAt = java.time.LocalDateTime.now()
                )
            )
        }
    }

    @Query("UPDATE account SET isActive = 0")
    suspend fun deactivateAllAccounts()

    @Query("SELECT COUNT(*) FROM account")
    suspend fun getAccountCount(): Int

    @Query("SELECT COUNT(*) FROM account WHERE isActive = 1")
    suspend fun hasActiveAccount(): Int
}
