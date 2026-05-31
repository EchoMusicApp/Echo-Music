

package iad1tya.echo.music.ui.screens.wrapped

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import iad1tya.echo.music.db.DatabaseDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

internal val LocalWrappedManager = compositionLocalOf<WrappedManager> { error("No WrappedManager found!") }

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WrappedEntryPoint {
    fun databaseDao(): DatabaseDao
}

internal fun provideWrappedManager(context: Context): WrappedManager {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WrappedEntryPoint::class.java
    )
    return WrappedManager(
        databaseDao = entryPoint.databaseDao(),
        context = context.applicationContext
    )
}
