package underscore.andthereitgoes.shadepile.transpiler.lua.runtime.lib;


import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class StringLib {

  public static Integer[] byte_(String s, Optional<Long> i, Optional<Long> j) {
    long startP1 = i.orElse(1L);
    int start = (int)(startP1 - 1L);
    int end = (int)(long)j.orElse(startP1);
    start = Math.clamp(start, 0, s.length() - 1);
    end = Math.clamp(end, 0, s.length() - 1);
    if (end < start) return new Integer[0];
    return s.substring(start, end).chars().boxed().toArray(Integer[]::new);
  }

  public static int len(String s) { return s.length(); }

  public static String lower(String s) { return s.toLowerCase(); }

  public static String rep(String s, int n, Optional<String> sep) {
    final String seps;
    if (sep.isEmpty() || (seps = sep.get()).isEmpty()) return s.repeat(n);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      if (i != 0) sb.append(seps);
      sb.append(s);
    }
    return sb.toString();
  }

  public static String reverse(String s) {
    return String.valueOf(List.of(s.toCharArray()).reversed());
  }

  public static String sub(String s, int i, Optional<Integer> j) {
    int jn = j.orElse(-1);
    if (i < 0) i += s.length();
    if (jn < 0) jn += s.length();
    i = Math.clamp(i + 1, 0, s.length() - 1);
    jn = Math.clamp(jn + 1, 0, s.length() - 1);
    return s.substring(i, jn);
  }

  public static String upper(String s) { return s.toUpperCase(); }
}
