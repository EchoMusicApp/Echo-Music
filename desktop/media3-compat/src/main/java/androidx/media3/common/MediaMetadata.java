package androidx.media3.common;

import android.net.Uri;
import android.os.Bundle;
import org.jetbrains.annotations.Nullable;

/** Metadata of a media item (same shape as Media3's MediaMetadata for the fields the app uses). */
public final class MediaMetadata {
  public static final int MEDIA_TYPE_MIXED = 0;
  public static final int MEDIA_TYPE_MUSIC = 1;
  public static final int MEDIA_TYPE_AUDIO_BOOK_CHAPTER = 2;
  public static final int MEDIA_TYPE_PODCAST_EPISODE = 3;
  public static final int MEDIA_TYPE_RADIO_STATION = 4;
  public static final int MEDIA_TYPE_VIDEO = 8;
  public static final int MEDIA_TYPE_FOLDER_MIXED = 11;
  public static final int MEDIA_TYPE_FOLDER_ALBUMS = 12;
  public static final int MEDIA_TYPE_FOLDER_ARTISTS = 13;
  public static final int MEDIA_TYPE_FOLDER_GENRES = 14;
  public static final int MEDIA_TYPE_FOLDER_PLAYLISTS = 15;
  public static final int MEDIA_TYPE_FOLDER_YEARS = 16;
  public static final int MEDIA_TYPE_FOLDER_AUDIO_BOOKS = 17;
  public static final int MEDIA_TYPE_FOLDER_PODCASTS = 18;
  public static final int MEDIA_TYPE_FOLDER_RADIO_STATIONS = 19;

  public static final MediaMetadata EMPTY = new Builder().build();

  @Nullable public final CharSequence title;
  @Nullable public final CharSequence artist;
  @Nullable public final CharSequence albumTitle;
  @Nullable public final CharSequence albumArtist;
  @Nullable public final CharSequence displayTitle;
  @Nullable public final CharSequence subtitle;
  @Nullable public final CharSequence description;
  @Nullable public final byte[] artworkData;
  @Nullable public final Uri artworkUri;
  @Nullable public final Integer trackNumber;
  @Nullable public final Integer totalTrackCount;
  @Nullable public final Boolean isBrowsable;
  @Nullable public final Boolean isPlayable;
  @Nullable public final Integer mediaType;
  @Nullable public final Bundle extras;

  private MediaMetadata(Builder b) {
    title = b.title; artist = b.artist; albumTitle = b.albumTitle; albumArtist = b.albumArtist;
    displayTitle = b.displayTitle; subtitle = b.subtitle; description = b.description;
    artworkData = b.artworkData; artworkUri = b.artworkUri; trackNumber = b.trackNumber;
    totalTrackCount = b.totalTrackCount; isBrowsable = b.isBrowsable; isPlayable = b.isPlayable;
    mediaType = b.mediaType; extras = b.extras;
  }

  public Builder buildUpon() { return new Builder(this); }

  public static final class Builder {
    @Nullable private CharSequence title, artist, albumTitle, albumArtist, displayTitle, subtitle, description;
    @Nullable private byte[] artworkData;
    @Nullable private Uri artworkUri;
    @Nullable private Integer trackNumber, totalTrackCount, mediaType;
    @Nullable private Boolean isBrowsable, isPlayable;
    @Nullable private Bundle extras;

    public Builder() {}

    private Builder(MediaMetadata m) {
      title = m.title; artist = m.artist; albumTitle = m.albumTitle; albumArtist = m.albumArtist;
      displayTitle = m.displayTitle; subtitle = m.subtitle; description = m.description;
      artworkData = m.artworkData; artworkUri = m.artworkUri; trackNumber = m.trackNumber;
      totalTrackCount = m.totalTrackCount; isBrowsable = m.isBrowsable; isPlayable = m.isPlayable;
      mediaType = m.mediaType; extras = m.extras;
    }

    public Builder setTitle(@Nullable CharSequence v) { title = v; return this; }
    public Builder setArtist(@Nullable CharSequence v) { artist = v; return this; }
    public Builder setAlbumTitle(@Nullable CharSequence v) { albumTitle = v; return this; }
    public Builder setAlbumArtist(@Nullable CharSequence v) { albumArtist = v; return this; }
    public Builder setDisplayTitle(@Nullable CharSequence v) { displayTitle = v; return this; }
    public Builder setSubtitle(@Nullable CharSequence v) { subtitle = v; return this; }
    public Builder setDescription(@Nullable CharSequence v) { description = v; return this; }
    public Builder setArtworkData(@Nullable byte[] v, @Nullable Integer pictureType) { artworkData = v; return this; }
    public Builder setArtworkUri(@Nullable Uri v) { artworkUri = v; return this; }
    public Builder setTrackNumber(@Nullable Integer v) { trackNumber = v; return this; }
    public Builder setTotalTrackCount(@Nullable Integer v) { totalTrackCount = v; return this; }
    public Builder setIsBrowsable(@Nullable Boolean v) { isBrowsable = v; return this; }
    public Builder setIsPlayable(@Nullable Boolean v) { isPlayable = v; return this; }
    public Builder setMediaType(@Nullable Integer v) { mediaType = v; return this; }
    public Builder setExtras(@Nullable Bundle v) { extras = v; return this; }
    public MediaMetadata build() { return new MediaMetadata(this); }
  }
}
