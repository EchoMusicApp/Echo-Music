package androidx.media3.datasource.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Desktop placeholder for Media3's on-disk stream cache: remembers which keys are cached (none until a real cache is wired in). */
public class SimpleCache {
  private final Set<String> cached = ConcurrentHashMap.newKeySet();
  public Set<String> getKeys() { return cached; }
  public boolean isCached(String key, long position, long length) { return cached.contains(key); }
  public void removeResource(String key) { cached.remove(key); }
  public void release() {}
  public long getCacheSpace() { return 0; }
}
