package androidx.media3.exoplayer.offline;

import android.content.Context;

/** Entry points the UI calls to start/stop downloads; the desktop download manager sets [handler] when it exists. */
public class DownloadService {
  public static final String CHANNEL_ID = "download";
  public static final int STOP_REASON_NONE = 0;

  public interface Handler {
    void add(DownloadRequest request, boolean foreground);
    void remove(String id);
    void removeAll();
  }

  /** shortcut: null until the desktop downloader is wired in, so downloads are silently ignored. */
  public static volatile Handler handler;

  public static void sendAddDownload(Context context, Class<?> clazz, DownloadRequest request, boolean foreground) { Handler h = handler; if (h != null) h.add(request, foreground); }
  public static void sendRemoveDownload(Context context, Class<?> clazz, String id, boolean foreground) { Handler h = handler; if (h != null) h.remove(id); }
  public static void sendRemoveAllDownloads(Context context, Class<?> clazz, boolean foreground) { Handler h = handler; if (h != null) h.removeAll(); }
}
