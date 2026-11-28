package androidx.media3.common;

import java.util.Collections;
import java.util.List;

public final class Tracks {
  public static final Tracks EMPTY = new Tracks(Collections.emptyList());
  public static final class Group {
    public final int type;
    private final Format format;
    public Group(int type, Format format) { this.type = type; this.format = format; }
    public int getType() { return type; }
    public Format getTrackFormat(int index) { return format; }
  }
  private final List<Group> groups;
  public Tracks(List<Group> groups) { this.groups = groups; }
  public List<Group> getGroups() { return groups; }
}
