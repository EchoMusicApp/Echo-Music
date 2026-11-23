package android.net;

public abstract class Uri {
  public static final Uri EMPTY = new StringUri("");

  public static Uri parse(String uriString) {
    return new StringUri(uriString);
  }

  public abstract String toString();

  private static class StringUri extends Uri {
    private final String uriString;

    StringUri(String uriString) {
      this.uriString = uriString != null ? uriString : "";
    }

    @Override
    public String toString() {
      return uriString;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Uri)) return false;
      return uriString.equals(o.toString());
    }

    @Override
    public int hashCode() {
      return uriString.hashCode();
    }
  }
}
