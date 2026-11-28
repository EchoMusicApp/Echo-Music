package androidx.media3.datasource;

import android.net.Uri;
import java.util.Collections;
import java.util.Map;

/** Just the request description (URL + headers) that the stream resolver rewrites. */
public final class DataSpec {
  public final Uri uri;
  public final Map<String, String> httpRequestHeaders;

  public DataSpec(Uri uri) { this(uri, Collections.emptyMap()); }
  public DataSpec(Uri uri, Map<String, String> headers) { this.uri = uri; this.httpRequestHeaders = headers; }
  public Builder buildUpon() { return new Builder(uri, httpRequestHeaders); }

  public static final class Builder {
    private Uri uri;
    private Map<String, String> headers;
    Builder(Uri uri, Map<String, String> headers) { this.uri = uri; this.headers = headers; }
    public Builder setUri(Uri uri) { this.uri = uri; return this; }
    public Builder setHttpRequestHeaders(Map<String, String> headers) { this.headers = headers; return this; }
    public DataSpec build() { return new DataSpec(uri, headers); }
  }
}
