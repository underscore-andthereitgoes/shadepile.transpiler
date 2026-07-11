package underscore.andthereitgoes.shadepile.transpiler.lua.runtime;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;


@SuppressWarnings("unused")
public class LuaRuntime {

  public static final Object noKey = new Object();

  public static @NotNull Number tonumber_raw(String e) {
    if (e.startsWith("-")) return tonumber_raw(e.substring(1));
    e = e.toLowerCase(Locale.ROOT);
    if (e.startsWith("0x")) {
      try {
        String[] slices = e.substring(2).split("p", 2);
        String[] numSSlices = slices[0].split("\\.", 2);
        @Nullable String expS = slices.length > 1 ? slices[1] : null;
        @NotNull String intS = numSSlices[0];
        @Nullable String fracS = numSSlices.length > 1 ? numSSlices[1] : null;
        long intPart = Long.parseLong(intS, 16);
        if (fracS != null || expS != null) {
          long fracPart = fracS == null ? 0L : Long.parseLong(fracS, 16);
          long expPart = expS == null ? 0L : Long.parseLong(expS, 16);
          return ((double)intPart + (fracS != null ? (double)fracPart / Math.pow(16d, fracS.length()) : 0d)) * (expS == null ? 1d : Math.pow(2d, expS.length()));
        } else {
          return intPart;
        }
      } catch (NumberFormatException ignored) {
        return Double.NaN;
      }
    } else {
      try {
        if (e.equals("infinity") || e.equals("nan")) throw new NumberFormatException();
        if (e.startsWith("-") || e.startsWith("+")) throw new NumberFormatException();
        if (e.endsWith("f") || e.endsWith("d")) throw new NumberFormatException();
        return Double.parseDouble(e);
      } catch (NumberFormatException ignored) {
        return Double.NaN;
      }
    }
  }
  public static @NotNull Number tonumber_raw(String e, int base) {
    if (e.startsWith("-")) return tonumber_raw(e.substring(1), base);
    e = e.toLowerCase(Locale.ROOT);
    try {
      if (base < 2 || base > 36) throw new NumberFormatException();
      if (e.startsWith("-") || e.startsWith("+")) throw new NumberFormatException();
      if (base < 22 && e.endsWith("l")) throw new NumberFormatException();
      return Long.parseLong(e, base);
    } catch (NumberFormatException ignored) {
      return Double.NaN;
    }
  }

  /// Accepts an argument as a `Double` or `Long` if possible (without string conversion), or throws an error if not.
  public static Number assertNumber(Object argument, String methodName, int argumentIndex) {
    if (argument instanceof Double d) return d;
    if (argument instanceof Long l) return l;
    throw new LuaRuntimeError(methodName + " expected a number for argument " + argumentIndex);
  }

  /// Converts an argument to a `double` if possible (without string conversion), or throws an error if not.
  public static double assertDouble(Object argument, String methodName, int argumentIndex) {
    if (argument instanceof Double d) return d;
    if (argument instanceof Long l) return l.doubleValue();
    throw new LuaRuntimeError(methodName + " expected a number for argument " + argumentIndex);
  }

  /// Converts an argument to a `long` if possible (without string conversion), or throws an error if not.
  public static long assertInteger(Object argument, String methodName, int argumentIndex) {
    if (argument instanceof Long l) return l;
    if (argument instanceof Double d && !d.isNaN() && !d.isInfinite() && d == Math.floor(d)) return d.longValue();
    throw new LuaRuntimeError(methodName + " expected an integer for argument " + argumentIndex);
  }

  public final Utils u;
  public final Funcs f;
  public final Ops o;
  public final Props p;

  public LuaRuntime() {
    u = new Utils();
    f = new Funcs();
    o = new Ops();
    p = new Props();
  }

  public class Utils {
    private Utils() {}

    private @Nullable WeakReference<Object[]> restJustExtracted = null;
    private int restJustExtractedPrefixCount = 0;
    private @Nullable WeakReference<@Nullable Object @NotNull []> restExtractionResult = null;

    private @Nullable Object @NotNull [] extractRestInternal(Object[] args, int prefixArgCount) {
      if (args.length <= prefixArgCount) return new Object[0];
      Object[] rest = new Object[args.length - prefixArgCount];
      System.arraycopy(args, prefixArgCount, rest, 0, rest.length);
      return rest;
    }

    public @Nullable Object @NotNull [] extractRest(Object[] args, int prefixArgCount) {
      if (
          this.restJustExtracted != null &&
          this.restExtractionResult != null &&
          this.restJustExtracted.refersTo(args) &&
          this.restJustExtractedPrefixCount == prefixArgCount
      ) {
        Object[] cached = this.restExtractionResult.get();
        if (cached != null) return cached;
      }
      this.restJustExtracted = new WeakReference<>(args);
      this.restJustExtractedPrefixCount = prefixArgCount;
      Object[] result = extractRestInternal(args, prefixArgCount);
      this.restExtractionResult = new WeakReference<>(result);
      return result;
    }

    public @NotNull LuaTable extractRestTable(Object[] args, int prefixArgCount) {
      return LuaTable.ofListN(extractRest(args, prefixArgCount));
    }

    public @Nullable Object @NotNull [] multires(@Nullable Object... parameters) {
      if (parameters.length == 0) return parameters;

      int lastI = parameters.length - 1;
      @Nullable Object lastP = parameters[lastI];

      final @Nullable Object @NotNull [] parameters2;

      if (lastP instanceof Object[] lastPMulti) {
        parameters2 = new Object[lastI + lastPMulti.length];
        for (int i = 0; i < lastI; i++) {
          Object p = parameters[i];
          if (p instanceof Object[] pMulti) parameters2[lastI] = pMulti.length > 0 ? pMulti[0] : null;
          else parameters2[lastI] = p;
        }
        System.arraycopy(lastPMulti, 0, parameters2, lastI, lastPMulti.length);
      } else {
        parameters2 = new Object[parameters.length];
        for (Object p: parameters) {
          if (p instanceof Object[] pMulti) parameters2[lastI] = pMulti.length > 0 ? pMulti[0] : null;
          else parameters2[lastI] = p;
        }
      }

      return parameters2;
    }

    public boolean single(boolean any) { return any; }
    public double single(double any) { return any; }
    public long single(long any) { return any; }
    public String single(String any) { return any; }
    public @Nullable Object single(@Nullable Object any) {
      return any instanceof Object[] multi ? (multi.length > 0 ? multi[0] : null) : any;
    }
    public @NotNull Number tonumber_raw(String e) {
      return LuaRuntime.tonumber_raw(e);
    }
    public @NotNull Number tonumber_raw(String e, int base) {
      return LuaRuntime.tonumber_raw(e, base);
    }

    public LuaTable table(@Nullable Object... flatEntries) {
      LuaTable table = new LuaTable();
      int listIterator = 1;
      int lastEntry = flatEntries.length - 2;
      for (int i = 0; i < flatEntries.length - 1; i += 2) {
        Object oKey = flatEntries[i];
        Object oValue = flatEntries[i+1];
        if (i == lastEntry && oKey == noKey && oValue instanceof Object[] multires) {
          for (Object single: multires) {
            long key = listIterator++;
            LuaPropertyReference ref = LuaRuntime.this.p.ref(table, key);
            Objects.requireNonNull(ref.target).put(key, single);
          }
        } else {
          Object value = single(oValue);
          if (oKey == noKey) {
            long key = listIterator++;
            LuaPropertyReference ref = LuaRuntime.this.p.ref(table, key);
            Objects.requireNonNull(ref.target).put(key, value);
          } else {
            Object key = single(oKey);
            LuaPropertyReference ref = LuaRuntime.this.p.ref(table, key);
            Objects.requireNonNull(ref.target).put(key, value);
          }
        }
      }
      return table;
    }
  }

  public class Funcs {
    private Funcs() {}

    public Function<Object[],Object[]> def(Function<Object[],Object[]> f) {
      return f;
    }

    public Function<Object[],Object[]> fbind(Object self, Object target, Object... parameters) {
      Object oTarget = target;
      if (target instanceof LuaTableOrUserdata) target = LuaRuntime.this.o.tofunc(target);
      if (target instanceof Function<?,?> func) {
        //noinspection unchecked
        final Function<Object[],Object[]> f = (Function<Object[],Object[]>)func;
        Object[] plist;
        if (self == null) plist = LuaRuntime.this.u.multires(parameters);
        else {
          ArrayList<Object> l = new ArrayList<>(List.of(self));
          l.addAll(Arrays.asList(parameters));
          plist = LuaRuntime.this.u.multires(l.toArray());
        }
        return f.compose((Object _) -> LuaRuntime.this.u.multires(plist));
      } else {
        throw new LuaRuntimeError(LuaRuntime.this.o.type(oTarget) + " is not callable");
      }
    }

    public Object[] callbound(Function<Object[],Object[]> func) {
      Object[] ret = func.apply(new Object[0]);
      if (ret == null) ret = new Object[0];
      return ret;
    }

    public Object[] fcall(Object target, Object... parameters) {
      return LuaRuntime.this.f.callbound(LuaRuntime.this.f.fbind(null, target, parameters));
    }

    public Iterable<Object[]> iterator(Object initRaw) {
      final Object[] init = initRaw instanceof Object[] ? (Object[])initRaw : new Object[]{initRaw};
      final Object iterFunc = init.length > 0 ? init[0] : null;
      if (init.length > 3) {
        throw new UnsupportedOperationException("to-be-closed variables (fourth item in generic for initialiser) are currently not supported. sorry :(");
      }
      return () -> new Iterator<>() {
        private final Object state = init.length > 1 ? init[1] : null;
        private Object control = init.length > 2 ? init[2] : null;
//        private Object closable = init.length > 3 ? init[3] : null;
//        private boolean closed = false;
        private Object[] values = null;
        @Override
        public boolean hasNext() {
          return values == null || control != null;
        }
        @Override
        public Object[] next() {
          final Object[] v = values;
          values = LuaRuntime.this.u.multires(LuaRuntime.this.f.fcall(null, iterFunc, state, control));
          control = init.length > 0 ? init[0] : null;
          return v;
        }
      };
    }

    public Iterable<Double> numericIterator(double start, double stop, double step) {
      if (step == 0.0d) throw new LuaRuntimeError("numeric for step cannot be zero");
      if (step < 0.0d) {
        return () -> new PrimitiveIterator.OfDouble() {
          private double i = start; // value to be yielded next
          @Override
          public boolean hasNext() {
            return i >= stop;
          }
          @Override
          public double nextDouble() {
            return i += step;
          }
        };
      } else {
        return () -> new PrimitiveIterator.OfDouble() {
          private double i = start;
          @Override
          public boolean hasNext() {
            return i <= stop;
          }
          @Override
          public double nextDouble() {
            return i += step;
          }
        };
      }
    }
    public Iterable<Long> numericIterator(long start, long stop, long step) {
      if (step == 0L) throw new LuaRuntimeError("numeric for step cannot be zero");
      if (step < 0L) {
        return () -> new PrimitiveIterator.OfLong() {
          private long i = start; // value to be yielded next
          @Override
          public boolean hasNext() {
            return i >= stop;
          }
          @Override
          public long nextLong() {
            return i += step;
          }
        };
      } else {
        return () -> new PrimitiveIterator.OfLong() {
          private long i = start;
          @Override
          public boolean hasNext() {
            return i <= stop;
          }
          @Override
          public long nextLong() {
            return i += step;
          }
        };
      }
    }
    public Iterable<Double> numericIterator(double start, double stop) { return numericIterator(start, stop, 1d); }
    public Iterable<Double> numericIterator(double start, long stop) { return numericIterator(start, (double)stop, 1d); }
    public Iterable<Double> numericIterator(long start, double stop) { return numericIterator((double)start, stop, 1d); }
    public Iterable<Long> numericIterator(long start, long stop) { return numericIterator(start, stop, 1L); }
    public Iterable<Double> numericIterator(double start, double stop, long step) { return numericIterator(start, stop, (double)step); }
    public Iterable<Double> numericIterator(double start, long stop, double step) { return numericIterator(start, (double)stop, step); }
    public Iterable<Double> numericIterator(long start, double stop, double step) { return numericIterator((double)start, stop, step); }
    public Iterable<Double> numericIterator(double start, long stop, long step) { return numericIterator(start, (double)stop, (double)step); }
    public Iterable<Double> numericIterator(long start, double stop, long step) { return numericIterator((double)start, stop, (double)step); }
    public Iterable<Double> numericIterator(long start, long stop, double step) { return numericIterator((double)start, (double)stop, step); }
    public Iterable<?> numericIterator(long start, Object stop) {
      if (stop instanceof Number n)
        if (stop instanceof Long l) return numericIterator(start, l.longValue(), 1L);
        else return numericIterator((double)start, n.doubleValue(), 1d);
      throw new LuaRuntimeError("expected numbers in for loop, not " + LuaRuntime.this.o.type(stop));
    }
    public Iterable<Double> numericIterator(double start, Object stop) {
      if (stop instanceof Number n) return numericIterator(start, n.doubleValue(), 1d);
      throw new LuaRuntimeError("expected numbers in for loop, not " + LuaRuntime.this.o.type(stop));
    }
    public Iterable<?> numericIterator(Object start, long stop) {
      if (start instanceof Number n)
        if (start instanceof Long l) return numericIterator(l.longValue(), stop, 1L);
        else return numericIterator(n.doubleValue(), (double)stop, 1d);
      throw new LuaRuntimeError("expected numbers in for loop, not " + LuaRuntime.this.o.type(start));
    }
    public Iterable<Double> numericIterator(Object start, double stop) {
      if (start instanceof Number n) return numericIterator(n.doubleValue(), stop, 1d);
      throw new LuaRuntimeError("expected numbers in for loop, not " + LuaRuntime.this.o.type(start));
    }
    public Iterable<?> numericIterator(Object start, Object stop) {
      if (start instanceof Number n1 && stop instanceof Number n2)
        if (start instanceof Long l1 && stop instanceof Long l2) return numericIterator(l1.longValue(), l2.longValue(), 1L);
        else return numericIterator(n1.doubleValue(), n2.doubleValue(), 1d);
      throw new LuaRuntimeError("expected numbers in for loop, not " + LuaRuntime.this.o.type(start) + ", " + LuaRuntime.this.o.type(stop));
    }
    public Iterable<?> numericIterator(Object start, Object stop, Object step) {
      if (start instanceof Number n1 && stop instanceof Number n2 && step instanceof Number n3)
        if (start instanceof Long l1 && stop instanceof Long l2 && step instanceof Long l3) return numericIterator(l1.longValue(), l2.longValue(), l3.longValue());
        else return numericIterator(n1.doubleValue(), n2.doubleValue(), n3.doubleValue());
      throw new LuaRuntimeError("expected numbers in for loop, not " + LuaRuntime.this.o.type(start) + ", " + LuaRuntime.this.o.type(stop) + ", " + LuaRuntime.this.o.type(step));
    }

  }

  public class Ops {
    private Ops() {}

    protected final Object metamethodsNotImplemented = UnsupportedOperationException.class;

    public boolean isint(boolean x) { return false; }
    public boolean isint(double n) { return n >= (double)Long.MIN_VALUE && n <= (double)Long.MAX_VALUE && n == Math.floor(n); }
    public boolean isint(long n) { return true; }
    public boolean isint(String x) { return false; }
    public boolean isint(Boolean x) { return false; }
    public boolean isint(Double n) { return isint((double)n); }
    public boolean isint(Long n) { return true; }
    public boolean isint(Object v) {
      return switch (v) {
        case Double n -> isint((double)n);
        case Long _ -> true;
        case null, default -> false;
      };
    }

    public String type(Object item) {
      return switch (item) {
        case null -> "nil";
        case Number number -> "number";
        case Boolean b -> "boolean";
        case String s -> "string";
        case Function<?,?> function -> "function";
        case Thread thread -> "thread";
        default -> {
          if (item instanceof LuaTableOrUserdata table) {
            var type = LuaRuntime.this.p.getMetafield(table, "__type");
            if (type != null) {
              if (type instanceof Function<?,?> function || type instanceof LuaTableOrUserdata) {
                yield tostring(LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, tofunc(type), item)));
              } else {
                yield tostring(type);
              }
            }
          }
          yield item instanceof LuaTable ? "table" : "userdata";
        }
      };
    }

    @Contract(value = "_ -> param1", pure = true)
    public boolean asbool(boolean value) { return value; }
    @Contract(value = "null -> false", pure = true)
    public boolean asbool(Object value) {
      value = LuaRuntime.this.u.single(value);
      if (value instanceof Boolean bool) return bool;
      return value != null;
    }

    public Object and(Object left, Supplier<Object> right) {
      return LuaRuntime.this.u.single(asbool(left) ? right.get() : left);
    }

    public Object or(Object left, Supplier<Object> right) {
      return LuaRuntime.this.u.single(asbool(left) ? left : right.get());
    }

    public boolean not(Object operand) {
      operand = LuaRuntime.this.u.single(operand);
      return !asbool(operand);
    }

    public Object useSameMetamethod(@Nullable Object left, @Nullable Object right, String name) {
      if (left == null || right == null) return metamethodsNotImplemented;
      var mmLeft = LuaRuntime.this.p.getMetamethod(left, name);
      var mmRight = LuaRuntime.this.p.getMetamethod(right, name);
      if (mmLeft == mmRight) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mmLeft, left, right));
      return metamethodsNotImplemented;
    }

    public Function<Object[],Object[]> getEitherMetamethod(@Nullable Object left, @Nullable Object right, String name) {
      Function<Object[],Object[]> mmLeft = left == null ? null : LuaRuntime.this.p.getMetamethod(left, name);
      return mmLeft != null ? mmLeft : (right == null ? null : LuaRuntime.this.p.getMetamethod(right, name));
    }

    public boolean lt(double left, double right) { return left < right; }
    public boolean lt(long left, long right) { return left < right; }
    public boolean lt(double left, long right) { return left < (double)right; }
    public boolean lt(long left, double right) { return (double)left < right; }
    public boolean lt(Object left_, Object right_) {
      final Object left = LuaRuntime.this.u.single(left_); final Object right = LuaRuntime.this.u.single(right_);

      switch (left) {
        case Double a when right instanceof Double b -> {
          return a < b;
        }
        case Long a when right instanceof Long b -> {
          return a < b;
        }
        case Double a when right instanceof Long b -> {
          return a < b.doubleValue();
        }
        case Long a when right instanceof Double b -> {
          return a.doubleValue() < b;
        }
        case String a when right instanceof String b -> {
          return a.compareTo(b) < 0;
        }
        case null, default -> {
          var v = useSameMetamethod(left, right, "__lt");
          if (v != metamethodsNotImplemented) return asbool(v);
        }
      }

      throw new LuaRuntimeError("cannot compare " + type(left) + " to " + type(right));
    }

    public boolean gt(double left, double right) { return left > right; }
    public boolean gt(long left, long right) { return left > right; }
    public boolean gt(double left, long right) { return left > (double)right; }
    public boolean gt(long left, double right) { return (double)left > right; }
    public boolean gt(Object left_, Object right_) { return lt(right_, left_); }

    public boolean le(double left, double right) { return left <= right; }
    public boolean le(long left, long right) { return left <= right; }
    public boolean le(double left, long right) { return left <= (double)right; }
    public boolean le(long left, double right) { return (double)left <= right; }
    public boolean le(Object left_, Object right_) {
      final Object left = LuaRuntime.this.u.single(left_); final Object right = LuaRuntime.this.u.single(right_);

      switch (left) {
        case Double a when right instanceof Double b -> {
          return a <= b;
        }
        case Long a when right instanceof Long b -> {
          return a <= b;
        }
        case Double a when right instanceof Long b -> {
          return a <= b.doubleValue();
        }
        case Long a when right instanceof Double b -> {
          return a.doubleValue() <= b;
        }
        case String a when right instanceof String b -> {
          return a.compareTo(b) <= 0;
        }
        case null, default -> {
          var v = useSameMetamethod(left, right, "__le");
          if (v != metamethodsNotImplemented) return asbool(v);
          v = useSameMetamethod(right, left, "__lt");
          if (v != metamethodsNotImplemented) return !asbool(v);
        }
      }

      throw new LuaRuntimeError("cannot compare " + type(left) + " to " + type(right));
    }

    public boolean ge(double left, double right) { return left >= right; }
    public boolean ge(long left, long right) { return left >= right; }
    public boolean ge(double left, long right) { return left >= (double)right; }
    public boolean ge(long left, double right) { return (double)left >= right; }
    public boolean ge(Object left_, Object right_) { return le(right_, left_); }

    public boolean eq(double left, double right) { return left == right; }
    public boolean eq(long left, long right) { return left == right; }
    public boolean eq(double left, long right) { return left == (double)right; }
    public boolean eq(long left, double right) { return (double)left == right; }
    public boolean eq(Object left_, Object right_) {
      final Object left = LuaRuntime.this.u.single(left_); final Object right = LuaRuntime.this.u.single(right_);

      switch (left) {
        case Double a when right instanceof Double b -> {
          return a <= b;
        }
        case Long a when right instanceof Long b -> {
          return a <= b;
        }
        case Double a when right instanceof Long b -> {
          return a <= b.doubleValue();
        }
        case Long a when right instanceof Double b -> {
          return a.doubleValue() <= b;
        }
        case String a when right instanceof String b -> {
          return a.compareTo(b) <= 0;
        }
        case null, default -> {
          var v = useSameMetamethod(left, right, "__eq");
          if (v != metamethodsNotImplemented) return asbool(v);
          return (left == null && right == null) || Objects.equals(left, right);
        }
      }
    }

    public boolean ne(double left, double right) { return left != right; }
    public boolean ne(long left, long right) { return left != right; }
    public boolean ne(double left, long right) { return left != (double)right; }
    public boolean ne(long left, double right) { return (double)left != right; }
    public boolean ne(Object left_, Object right_) { return !eq(left_, right_); }

    public double add(double left, double right) { return left + right; }
    public double add(long left, double right) { return (double)left + right; }
    public double add(double left, long right) { return left + (double)right; }
    public long   add(long left, long right) { return left + right; }
    public double add(Double left, Double right) { return (double)left + right; }
    public long   add(Long left, Long right) { return (long)left + right; }
    public double add(Number left, Number right) { return left.doubleValue() + right.doubleValue(); }
    public Object add(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__add";

      if (left instanceof Long a && right instanceof Long b) return a + b;
      if (left instanceof Number a && right instanceof Number b) return a.doubleValue() + b.doubleValue();

      boolean ln = left instanceof Number, rn = right instanceof Number;
      boolean ls = left instanceof String, rs = right instanceof String;

      //noinspection LoopStatementThatDoesntLoop
      while ((ln || ls) && (rn || rs)) {
        Number l;
        Number r;
        if (ls) {
          l = LuaRuntime.this.u.tonumber_raw((String)left);
          if (Double.isNaN(l.doubleValue())) break;
        } else l = (Number)left;
        if (rs) {
          r = LuaRuntime.this.u.tonumber_raw((String)right);
          if (Double.isNaN(r.doubleValue())) break;
        } else r = (Number)right;
        if (l instanceof Long a && r instanceof Long b) {
          return (long)a + b;
        }
        return l.doubleValue() + r.doubleValue();
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use + (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public double sub(double left, double right) { return left - right; }
    public double sub(long left, double right) { return (double)left - right; }
    public double sub(double left, long right) { return left - (double)right; }
    public long   sub(long left, long right) { return left - right; }
    public double sub(Double left, Double right) { return (double)left - right; }
    public long   sub(Long left, Long right) { return (long)left - right; }
    public double sub(Number left, Number right) { return left.doubleValue() - right.doubleValue(); }
    public Object sub(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__sub";

      if (left instanceof Long a && right instanceof Long b) return a - b;
      if (left instanceof Number a && right instanceof Number b) return a.doubleValue() - b.doubleValue();

      boolean ln = left instanceof Number, rn = right instanceof Number;
      boolean ls = left instanceof String, rs = right instanceof String;

      //noinspection LoopStatementThatDoesntLoop
      while ((ln || ls) && (rn || rs)) {
        Number l;
        Number r;
        if (ls) {
          l = LuaRuntime.this.u.tonumber_raw((String)left);
          if (Double.isNaN(l.doubleValue())) break;
        } else l = (Number)left;
        if (rs) {
          r = LuaRuntime.this.u.tonumber_raw((String)right);
          if (Double.isNaN(r.doubleValue())) break;
        } else r = (Number)right;
        if (l instanceof Long a && r instanceof Long b) {
          return (long)a - b;
        }
        return l.doubleValue() - r.doubleValue();
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use - (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public double mul(double left, double right) { return left * right; }
    public double mul(long left, double right) { return (double)left * right; }
    public double mul(double left, long right) { return left * (double)right; }
    public long   mul(long left, long right) { return left * right; }
    public double mul(Double left, Double right) { return (double)left * right; }
    public long   mul(Long left, Long right) { return (long)left * right; }
    public double mul(Number left, Number right) { return left.doubleValue() * right.doubleValue(); }
    public Object mul(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__mul";

      if (left instanceof Long a && right instanceof Long b) return a * b;
      if (left instanceof Number a && right instanceof Number b) return a.doubleValue() * b.doubleValue();

      boolean ln = left instanceof Number, rn = right instanceof Number;
      boolean ls = left instanceof String, rs = right instanceof String;

      //noinspection LoopStatementThatDoesntLoop
      while ((ln || ls) && (rn || rs)) {
        Number l;
        Number r;
        if (ls) {
          l = LuaRuntime.this.u.tonumber_raw((String)left);
          if (Double.isNaN(l.doubleValue())) break;
        } else l = (Number)left;
        if (rs) {
          r = LuaRuntime.this.u.tonumber_raw((String)right);
          if (Double.isNaN(r.doubleValue())) break;
        } else r = (Number)right;
        if (l instanceof Long a && r instanceof Long b) {
          return (long)a * b;
        }
        return l.doubleValue() * r.doubleValue();
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use * (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public double div(double left, double right) { return left / right; }
    public double div(long left, double right) { return (double)left / right; }
    public double div(double left, long right) { return left / (double)right; }
    public double div(long left, long right) { return (double)left / (double)right; }
    public double div(Double left, Double right) { return (double)left / right; }
    public double div(Number left, Number right) { return left.doubleValue() / right.doubleValue(); }
    public Object div(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__div";

      if (left instanceof Number a && right instanceof Number b) return a.doubleValue() / b.doubleValue();

      boolean ln = left instanceof Number, rn = right instanceof Number;
      boolean ls = left instanceof String, rs = right instanceof String;

      //noinspection LoopStatementThatDoesntLoop
      while ((ln || ls) && (rn || rs)) {
        Number l;
        Number r;
        if (ls) {
          l = LuaRuntime.this.u.tonumber_raw((String)left);
          if (Double.isNaN(l.doubleValue())) break;
        } else l = (Number)left;
        if (rs) {
          r = LuaRuntime.this.u.tonumber_raw((String)right);
          if (Double.isNaN(r.doubleValue())) break;
        } else r = (Number)right;
        return l.doubleValue() / r.doubleValue();
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use / (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public double pow(double left, double right) { return Math.pow(left, right); }
    public double pow(long left, double right) { return Math.pow((double)left, right); }
    public double pow(double left, long right) { return Math.pow(left, (double)right); }
    public double pow(long left, long right) { return Math.pow((double)left, (double)right); }
    public double pow(Double left, Double right) { return Math.pow(left, right); }
    public double pow(Number left, Number right) { return Math.pow(left.doubleValue(), right.doubleValue()); }
    public Object pow(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__pow";

      if (left instanceof Long a) left = a.doubleValue();
      if (right instanceof Long b) right = b.doubleValue();
      if (left instanceof Number a && right instanceof Number b) return Math.pow(a.doubleValue(), b.doubleValue());

      boolean ln = left instanceof Number, rn = right instanceof Number;
      boolean ls = left instanceof String, rs = right instanceof String;

      //noinspection LoopStatementThatDoesntLoop
      while ((ln || ls) && (rn || rs)) {
        Number l;
        Number r;
        if (ls) {
          l = LuaRuntime.this.u.tonumber_raw((String)left);
          if (Double.isNaN(l.doubleValue())) break;
        } else l = (Number)left;
        if (rs) {
          r = LuaRuntime.this.u.tonumber_raw((String)right);
          if (Double.isNaN(r.doubleValue())) break;
        } else r = (Number)right;
        return Math.pow(l.doubleValue(), r.doubleValue());
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use ^ (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public double idiv(double left, double right) { return Math.floor(left / right); }
    public double idiv(long left, double right) { return Math.floor((double)left / right); }
    public double idiv(double left, long right) { return Math.floor(left / (double)right); }
    public long   idiv(long left, long right) { return left / right; }
    public double idiv(Double left, Double right) { return Math.floor((double)left / right); }
    public long   idiv(Long left, Long right) { return (long)left / right; }
    public double idiv(Number left, Number right) { return Math.floor(left.doubleValue() / right.doubleValue()); }
    public Object idiv(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__idiv";

      if (left instanceof Long a && right instanceof Long b) return a / b;
      if (left instanceof Number a && right instanceof Number b) return Math.floor(a.doubleValue() / b.doubleValue());

      boolean ln = left instanceof Number, rn = right instanceof Number;
      boolean ls = left instanceof String, rs = right instanceof String;

      //noinspection LoopStatementThatDoesntLoop
      while ((ln || ls) && (rn || rs)) {
        Number l;
        Number r;
        if (ls) {
          l = LuaRuntime.this.u.tonumber_raw((String)left);
          if (Double.isNaN(l.doubleValue())) break;
        } else l = (Number)left;
        if (rs) {
          r = LuaRuntime.this.u.tonumber_raw((String)right);
          if (Double.isNaN(r.doubleValue())) break;
        } else r = (Number)right;
        if (l instanceof Long a && r instanceof Long b) {
          return (long)a / b;
        }
        return Math.floor(l.doubleValue() / r.doubleValue());
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use // (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public double mod(double left, double right) { return left - Math.floor(left / right) * right; }
    public double mod(long left, double right) { return (double)left - Math.floor((double)left / right) * right; }
    public double mod(double left, long right) { return left - Math.floor(left / (double)right) * (double)right; }
    public long   mod(long left, long right) { return Math.floorMod(left, right); }
    public double mod(Double left, Double right) { return left - Math.floor((double)left / right) * right; }
    public long   mod(Long left, Long right) { return Math.floorMod(left, right); }
    public double mod(Number left, Number right) { return mod(left.doubleValue(), right.doubleValue()); }
    public Object mod(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__mod";

      if (left instanceof Long a && right instanceof Long b) return Math.floorMod(a, b);
      if (left instanceof Number a && right instanceof Number b) return mod(a.doubleValue(), b.doubleValue());

      boolean ln = left instanceof Number, rn = right instanceof Number;
      boolean ls = left instanceof String, rs = right instanceof String;

      //noinspection LoopStatementThatDoesntLoop
      while ((ln || ls) && (rn || rs)) {
        Number l;
        Number r;
        if (ls) {
          l = LuaRuntime.this.u.tonumber_raw((String)left);
          if (Double.isNaN(l.doubleValue())) break;
        } else l = (Number)left;
        if (rs) {
          r = LuaRuntime.this.u.tonumber_raw((String)right);
          if (Double.isNaN(r.doubleValue())) break;
        } else r = (Number)right;
        if (l instanceof Long a && r instanceof Long b) {
          return Math.floorMod(a, b);
        }
        return mod(l.doubleValue(), r.doubleValue());
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use % (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public double unm(double operand) { return -operand; }
    public long   unm(long operand) { return -operand; }
    public double unm(Double operand) { return -(double)operand; }
    public long   unm(Long operand) { return -(long)operand; }
    public Object unm(Object operand) {
      operand = LuaRuntime.this.u.single(operand);

      final String mmn = "__unm";

      switch (operand) {
        case Long l -> {
          return -(long)l;
        }
        case Number n -> {
          return -n.doubleValue();
        }
        case String str -> {
          Number o = LuaRuntime.this.u.tonumber_raw(str);
          if (!Double.isNaN(o.doubleValue())) return o;
        }
        case null, default -> {
          var mm = LuaRuntime.this.p.getMetamethod(operand, mmn);
          if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, operand));
        }
      }
      throw new LuaRuntimeError("cannot use unary - (" + mmn + ") with " + type(operand));
    }

    public Object bnot(Object operand) {
      operand = LuaRuntime.this.u.single(operand);

      final String mmn = "__bnot";

      if (operand instanceof Double d) {
        if (d == Math.floor(d)) operand = d.longValue();
        else throw new LuaRuntimeError("cannot convert non-integral float to integer");
      }
      switch (operand) {
        case Long l -> {
          return ~(long)l;
        }
        case String str -> {
          Number n = LuaRuntime.this.u.tonumber_raw(str);
          if (n instanceof Long l) return ~(long)l;
        }
        case null, default -> {
          var mm = LuaRuntime.this.p.getMetamethod(operand, mmn);
          if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, operand));
        }
      }
      throw new LuaRuntimeError("cannot use unary ~ (" + mmn + ") with " + type(operand));
    }

    public Object band(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__band";

      if (left instanceof Double d) {
        if (d == Math.floor(d)) left = d.longValue();
        else throw new LuaRuntimeError("cannot convert non-integral float to integer");
      } else if (left instanceof String s) {
        try { left = Long.valueOf(s); } catch (NumberFormatException ignored) {}
      }
      if (right instanceof Double d) {
        if (d == Math.floor(d)) right = d.longValue();
        else throw new LuaRuntimeError("cannot convert non-integral float to integer");
      } else if (right instanceof String s) {
        try { right = Long.valueOf(s); } catch (NumberFormatException ignored) {}
      }

      if (left instanceof Long a && right instanceof Long b) return a & b;

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use & (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public Object bor(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__bor";

      if (left instanceof Double d) {
        if (d == Math.floor(d)) left = d.longValue();
        else throw new LuaRuntimeError("cannot convert non-integral float to integer");
      } else if (left instanceof String s) {
        try { left = Long.valueOf(s); } catch (NumberFormatException ignored) {}
      }
      if (right instanceof Double d) {
        if (d == Math.floor(d)) right = d.longValue();
        else throw new LuaRuntimeError("cannot convert non-integral float to integer");
      } else if (right instanceof String s) {
        try { right = Long.valueOf(s); } catch (NumberFormatException ignored) {}
      }

      if (left instanceof Long a && right instanceof Long b) return a | b;

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use | (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public Object bxor(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__bxor";

      if (left != null && right != null) {

        if (left instanceof Double d) {
          if (d == Math.floor(d)) left = d.longValue();
          else throw new LuaRuntimeError("cannot convert non-integral float to integer");
        } else if (left instanceof String s) {
          try { left = Long.valueOf(s); } catch (NumberFormatException ignored) {}
        }
        if (right instanceof Double d) {
          if (d == Math.floor(d)) right = d.longValue();
          else throw new LuaRuntimeError("cannot convert non-integral float to integer");
        } else if (right instanceof String s) {
          try { right = Long.valueOf(s); } catch (NumberFormatException ignored) {}
        }

        if (left instanceof Long a && right instanceof Long b) return a ^ b;

        var mm = getEitherMetamethod(left, right, mmn);
        if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));
      }
      throw new LuaRuntimeError("cannot use ~ (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public Object concat(Object left, Object right) {
      left = LuaRuntime.this.u.single(left); right = LuaRuntime.this.u.single(right);

      final String mmn = "__concat";

      if ((left instanceof Number || left instanceof String) && (right instanceof Number || right instanceof String)) {
        return left.toString() + right;
      }

      var mm = getEitherMetamethod(left, right, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, left, right));

      throw new LuaRuntimeError("cannot use .. (" + mmn + ") with " + type(left) + " and " + type(right));
    }

    public Object len(Object operand) {
      operand = LuaRuntime.this.u.single(operand);

      final String mmn = "__len";

      if (operand instanceof String s) return (long)s.length();

      var mm = LuaRuntime.this.p.getMetamethod(operand, mmn);
      if (mm != null) return LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(null, mm, operand));
      if (operand instanceof LuaTableOrUserdata t) return t.len();

      throw new LuaRuntimeError("cannot use unary # (" + mmn + ") with " + type(operand));
    }

    public String tostring(Object value) {
      value = LuaRuntime.this.u.single(value);
      switch (value) {
        case null -> {
          return "nil";
        }
        case String s -> {
          return s;
        }
        case Double d -> {
          if (d.isInfinite()) return d < 0d ? "-inf" : "inf";
          if (d.isNaN()) return "nan";
          return d.toString().toLowerCase(Locale.ROOT);
        }
        case Function<?,?> function -> {
          return "function: " + HexFormat.of().toHexDigits(function.hashCode());
        }
        case Thread thread -> {
          return "thread: " + HexFormat.of().toHexDigits(thread.hashCode());
        }
        case LuaTable table -> {
          Object tostringFunc = LuaRuntime.this.p.getMetamethod(table, "__tostring");
          if (tostringFunc != null) return tostring(LuaRuntime.this.f.fcall(null, tostringFunc, value));
          Object name = LuaRuntime.this.p.getMetafield(table, "__name");
          String stringName = HexFormat.of().toHexDigits(value.hashCode());
          if (name != null) stringName = tostring(name);
          return type(table) + ": " + stringName;
        }
        case LuaTableOrUserdata ud -> {
          return ud.toLuaString();
        }
        default -> {
          return value.toString();
        }
      }
    }

    public @Nullable Function<@Nullable Object @NotNull [], @Nullable Object @NotNull []> tofunc(Object value) {
      int depth = 0;
      while (value instanceof LuaTableOrUserdata t) {
        value = LuaRuntime.this.p.getMetamethod(t, "__call");
        if (++depth > 15) throw new LuaRuntimeError("maximum __call chain size exceeded (15)");
      }
      if (value instanceof Function<?,?> func) {
        //noinspection unchecked
        return (Function<@Nullable Object @NotNull [], @Nullable Object @NotNull []>)func;
      }
      return null;
    }
  }

  public class Props {
    private Props() {}

    public LuaTableOrUserdata primitiveMetatableNumber = new LuaTable().readonlyInLua();
    public LuaTableOrUserdata primitiveMetatableString = new LuaTable().readonlyInLua();
    public LuaTableOrUserdata primitiveMetatableBoolean = new LuaTable().readonlyInLua();
    public LuaTableOrUserdata primitiveMetatableFunction = new LuaTable().readonlyInLua();

    public LuaPropertyReference ref(Object target, long property) {
      target = LuaRuntime.this.u.single(target);
      if (target instanceof LuaTableOrUserdata table) {
        return new LuaPropertyReference(table, property);
      } else {
        throw new LuaRuntimeError("cannot get or set properties of " + LuaRuntime.this.o.type(target));
      }
    }
    public LuaPropertyReference ref(Object target, Object property) {
      target = LuaRuntime.this.u.single(target);
      if (target instanceof LuaTableOrUserdata table) {
        property = LuaRuntime.this.u.single(property);
        if (property == null || property.equals(Double.NaN)) throw new LuaRuntimeError("cannot use " + (property == null ? "nil" : "nan") + " as a key");
        if (property instanceof Double d) {
          if (Double.isFinite(d) && d == Math.floor(d)) property = d.longValue();
        }
        return new LuaPropertyReference(table, property);
      } else {
        throw new LuaRuntimeError("cannot get or set properties of " + LuaRuntime.this.o.type(target));
      }
    }
    public LuaPropertyReference refBind(Object target, Object property) {
      LuaPropertyReference r = ref(target, property);
      r.selfBinding = true;
      return r;
    }
    public LuaPropertyReference nowhere() {
      return LuaPropertyReference.nowhere();
    }

    public void set(LuaPropertyReference ref, Object value) {
      value = LuaRuntime.this.u.single(value);
      setRecurse(ref, value);
    }
    public void setRaw(LuaPropertyReference ref, Object value) {
      value = LuaRuntime.this.u.single(value);
      LuaTableOrUserdata target = ref.target;
      if (ref.isNowhere || target == null) return;
      if (target instanceof LuaTable luaTable && luaTable.isReadOnlyForLua) return;
      if (value == null) {
        target.remove(ref.property);
      } else {
        target.put(ref.property, value);
      }
    }
    private void setRecurse(LuaPropertyReference reference, Object value) {
      setRecurse(reference, reference.target, value, 0);
    }
    private void setRecurse(LuaPropertyReference originalReference, LuaTableOrUserdata currentTarget, Object value, int depth) {
      if (originalReference.isNowhere || currentTarget == null) return;
      if (depth > 100) throw new LuaRuntimeError("maximum __newindex chain size exceeded (100)");

      var newindex = getMetafield(currentTarget, "__newindex");

      if (newindex != null) {
        if (newindex instanceof LuaTableOrUserdata table) {
          setRecurse(originalReference, table, value, depth + 1);
          return;
        } else {
          var newindexFunc = LuaRuntime.this.o.tofunc(newindex);
          if (newindexFunc != null) {
            LuaRuntime.this.f.fcall(originalReference.target, newindexFunc, originalReference.property, value);
            return;
          }
        }
      }

      if (currentTarget instanceof LuaTable luaTable && luaTable.isReadOnlyForLua) return;

      if (value == null) {
        currentTarget.remove(originalReference.property);
      } else {
        currentTarget.put(originalReference.property, value);
      }
    }

    public @Nullable Object get(LuaPropertyReference reference) {
      return getRecurse(reference);
    }
    public @Nullable Object getRaw(LuaPropertyReference reference) {
      if (reference.isNowhere || reference.target == null) return null;
      return reference.target.get(reference.property);
    }
    private @Nullable Object getRecurse(LuaPropertyReference reference) {
      return getRecurse(reference, reference.target, 0);
    }
    private @Nullable Object getRecurse(LuaPropertyReference originalReference, LuaTableOrUserdata currentTarget, int depth) {
      if (originalReference.isNowhere || currentTarget == null) return null;
      if (depth > 100) throw new LuaRuntimeError("maximum __index chain size exceeded (100)");

      Object value = currentTarget.get(originalReference.property);

      if (value == null) {
        var indexer = getMetafield(currentTarget, "__index");
        if (indexer != null) {
          if (indexer instanceof LuaTableOrUserdata table) {
            return getRecurse(originalReference, table,  depth + 1);
          } else {
            var indexerFunc = LuaRuntime.this.o.tofunc(indexer);
            if (indexerFunc != null) {
              value = LuaRuntime.this.u.single(LuaRuntime.this.f.fcall(originalReference.target, indexerFunc, originalReference.property));
            }
          }
        }
      }

      if (originalReference.selfBinding) {
        // automatically throws error if not callable
        value = LuaRuntime.this.f.fbind(originalReference.target, value);
      }

      return value;
    }

    public @Nullable Object getMetafield(Object target, String field) {
      @Nullable LuaTableOrUserdata metatable;

      if (target instanceof LuaTableOrUserdata table) {
        metatable = table.getMetatable();
      } else {
        // primitive metatables are runtime-specific
        metatable = switch (target) {
          case Number number -> primitiveMetatableNumber;
          case String s -> primitiveMetatableString;
          case Boolean b -> primitiveMetatableBoolean;
          case Function<?,?> function -> primitiveMetatableFunction;
          case null, default -> null;
        };
      }

      if (metatable instanceof LuaTableOrUserdata mt)
        return LuaRuntime.this.p.get(LuaRuntime.this.p.ref(mt, field));
      return null;
    }

    public @Nullable Function<Object[],Object[]> getMetamethod(Object target, String name) {
      var func = getMetafield(target, name);
      if (func == null) return null;
      return LuaRuntime.this.o.tofunc(func);
    }
  }

}
