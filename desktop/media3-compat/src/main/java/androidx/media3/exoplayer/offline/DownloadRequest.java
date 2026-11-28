package androidx.media3.exoplayer.offline;

import android.net.Uri;
import org.jetbrains.annotations.Nullable;

public final class DownloadRequest {
  public final String id;
  public final Uri uri;
  @Nullable public final String customCacheKey;
  public final byte[] data;

  private DownloadRequest(Builder b) { id = b.id; uri = b.uri; customCacheKey = b.customCacheKey; data = b.data; }

  public static final class Builder {
    private final String id; private final Uri uri; private String customCacheKey; private byte[] data = new byte[0];
    public Builder(String id, Uri uri) { this.id = id; this.uri = uri; }
    public Builder setCustomCacheKey(@Nullable String key) { customCacheKey = key; return this; }
    public Builder setData(@Nullable byte[] data) { this.data = data == null ? new byte[0] : data; return this; }
    public Builder setMimeType(@Nullable String mimeType) { return this; }
    public DownloadRequest build() { return new DownloadRequest(this); }
  }
}
