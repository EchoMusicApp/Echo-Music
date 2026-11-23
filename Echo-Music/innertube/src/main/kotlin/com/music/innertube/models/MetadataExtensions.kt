package com.music.innertube.models

import com.music.echo.metadata.cleaner.MetadataCleaner
import com.music.echo.metadata.models.CleanedMetadata

val SongItem.cleanedMetadata: CleanedMetadata
  get() = MetadataCleaner.clean(title, artists.firstOrNull()?.name.orEmpty())

val SongItem.displayTitle: String
  get() = cleanedMetadata.toDisplayTitle()

val SongItem.cleanArtistName: String
  get() = cleanedMetadata.primaryArtist
