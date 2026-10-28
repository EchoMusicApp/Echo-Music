package com.music.echo.lossless

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LosslessEntryPoint {
  fun losslessSourcesRepository(): LosslessSourcesRepository
}
