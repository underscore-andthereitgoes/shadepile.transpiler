package underscore.andthereitgoes.shadepile.transpiler.lua.runtime;

import org.jetbrains.annotations.NotNull;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.lib.StringLib;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;


public abstract class LuaEnvironment extends LuaTable {

  private final @NotNull xoshiro256ss rng;
  private boolean usingRNG = false;
  private final @NotNull LuaRuntime runtime;

  public LuaRuntime getRuntime() {
    return runtime;
  }

  public LuaEnvironment(LuaRuntime runtime) {
    this.runtime = runtime;
    rng = new xoshiro256ss();
  }

  public abstract void print(Object[] args);

  public void addGlobals() {
    this.putFunction("print", objs -> {
      this.print(objs);
      return new Object[0];
    });
  }

  public LuaTable addMathModule() {
    LuaTable math = new LuaTable();

    math.putFunction("abs", (Object[] params) -> {
      Number n = LuaRuntime.assertNumber(params.length > 0 ? params[0] : null, "math.abs()", 1, runtime);
      if (n instanceof Long l) return new Object[]{Math.abs(l)};
      if (n instanceof Double d) return new Object[]{Math.abs(d)};
      throw new IllegalStateException();
    });
    math.putFunction("acos", (Object[] params) -> new Object[]{Math.acos(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.acos()", 1, runtime))});
    math.putFunction("asin", (Object[] params) -> new Object[]{Math.asin(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.asin()", 1, runtime))});
    math.putFunction("atan", (Object[] params) -> {
      double y = LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.atan()", 1, runtime);
      if (params.length >= 2) {
        double x = LuaRuntime.assertDouble(params[1], "math.atan()", 2, runtime);
        return new Object[]{Math.atan2(y, x)};
      }
      return new Object[]{Math.atan(y)};
    });
    math.putFunction("ceil", (Object[] params) -> {
      Number n = LuaRuntime.assertNumber(params.length > 0 ? params[0] : null, "math.ceil()", 1, runtime);
      if (n instanceof Long l) return new Object[]{l};
      if (n instanceof Double d) return new Object[]{(long)Math.ceil(d)};
      throw new IllegalStateException();
    });
    math.putFunction("cos", (Object[] params) -> new Object[]{Math.cos(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.cos()", 1, runtime))});
    math.putFunction("deg", (Object[] params) -> new Object[]{Math.toDegrees(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.deg()", 1, runtime))});
    math.putFunction("exp", (Object[] params) -> new Object[]{Math.exp(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.exp()", 1, runtime))});
    math.putFunction("floor", (Object[] params) -> {
      Number n = LuaRuntime.assertNumber(params.length > 0 ? params[0] : null, "math.floor()", 1, runtime);
      if (n instanceof Long l) return new Object[]{l};
      if (n instanceof Double d) return new Object[]{(long)Math.floor(d)};
      throw new IllegalStateException();
    });
    math.putFunction("fmod", (Object[] params) -> {
      Number a = LuaRuntime.assertNumber(params.length > 0 ? params[0] : null, "math.fmod()", 1, runtime);
      Number b = LuaRuntime.assertNumber(params.length > 1 ? params[1] : null, "math.fmod()", 2, runtime);
      if (a instanceof Double || b instanceof Double) {
        double x = a.doubleValue();
        double y = b.doubleValue();
        return new Object[]{x - (long)(x / y) * y};
      } else if (a instanceof Long x && b instanceof Long y) {
        return new Object[]{x - (x / y) * y}; // does truncation
      }
      throw new IllegalStateException();
    });
    math.putFunction("frexp", (Object[] params) -> {
      double x = LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.frexp()", 1, runtime);
      if (Double.isFinite(x) && x != 0d) {
        double e = Math.floor(Math.log(Math.abs(x)) / Math.log(2d) + 1d);
        double m = x / Math.pow(2d, e);
        return new Object[]{m, (long)e};
      } else {
        return new Object[]{x, 0L};
      }
    });
    math.put("huge", Double.POSITIVE_INFINITY);
    math.putFunction("ldexp", (Object[] params) -> {
      double m = LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.ldexp()", 1, runtime);
      int e = LuaRuntime.assertNumber(params.length > 1 ? params[1] : null, "math.ldexp()", 2, runtime).intValue();
      return new Object[]{m * (double)Math.powExact(2L, e)};
    });
    math.putFunction("log", (Object[] params) -> {
      double x = LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.log()", 1, runtime);
      if (params.length >= 2) {
        double base = LuaRuntime.assertDouble(params[1], "math.log()", 2, runtime);
        if (base == 10d) return new Object[]{Math.log10(x)};
        if (base != Math.E) return new Object[]{Math.log(x) / Math.log(base)};
      }
      return new Object[]{Math.log(x)};
    });
    math.putFunction("max", (Object[] params) -> {
      if (params.length == 0) throw new LuaRuntimeError("math.max() needs at least 1 argument");
      Object maximum = null;
      for (Object arg: params) {
        if (maximum == null || runtime.o.lt(maximum, arg)) maximum = arg;
      }
      return new Object[]{maximum};
    });
    math.put("maxinteger", Long.MAX_VALUE);
    math.putFunction("min", (Object[] params) -> {
      if (params.length == 0) throw new LuaRuntimeError("math.min() needs at least 1 argument");
      Object minimum = null;
      for (Object arg: params) {
        if (minimum == null || runtime.o.lt(arg, minimum)) minimum = arg;
      }
      return new Object[]{minimum};
    });
    math.put("mininteger", Long.MIN_VALUE);
    math.putFunction("modf", (Object[] params) -> {
      double x = LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.modf()", 1, runtime);
      if (x >= 0.0d) return new Object[]{Math.floor(x), x - Math.floor(x)};
      else return new Object[]{Math.ceil(x), x - Math.ceil(x)};
    });
    math.put("pi", Math.PI);
    math.putFunction("rad", (Object[] params) -> new Object[]{Math.toRadians(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.rad()", 1, runtime))});
    math.putFunction("random", (Object[] params) -> {
      if (this.usingRNG) {
        synchronized (this.rng) {
          if (params.length == 0) return new Object[]{this.rng.nextDouble()};
          long m = 1;
          long n;
          if (params.length == 1) {
            n = LuaRuntime.assertNumber(params[0], "math.random()", 1, runtime).longValue();
          } else {
            m = LuaRuntime.assertNumber(params[0], "math.random()", 1, runtime).longValue();
            n = LuaRuntime.assertNumber(params[1], "math.random()", 2, runtime).longValue();
          }
          return new Object[]{this.rng.nextLong(Math.abs(m - n) + 1L) + Math.min(m, n)};
        }
      } else {
        if (params.length == 0) return new Object[]{Math.random()};
        long m = 1;
        long n;
        if (params.length == 1) {
          n = LuaRuntime.assertNumber(params[0], "math.random()", 1, runtime).longValue();
        } else {
          m = LuaRuntime.assertNumber(params[0], "math.random()", 1, runtime).longValue();
          n = LuaRuntime.assertNumber(params[1], "math.random()", 2, runtime).longValue();
        }
        return new Object[]{RandomGenerator.getDefault().nextLong(Math.abs(m - n) + 1L) + Math.min(m, n)};
      }
    });
    math.putFunction("randomseed", (Object[] params) -> {
      synchronized (this.rng) {
        if (params.length == 0) {
          this.usingRNG = false; // use internal random instead of seeded random
        } else {
          long x = LuaRuntime.assertNumber(params[0], "math.randomseed()", 1, runtime).longValue();
          long y = params.length > 1 ? LuaRuntime.assertNumber(params[1], "math.randomseed()", 2, runtime).longValue() : 0;
          this.rng.seed(x, y);
          this.usingRNG = true;
        }
      }
      return new Object[0];
    });
    math.putFunction("sin", (Object[] params) -> new Object[]{Math.sin(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.sin()", 1, runtime))});
    math.putFunction("sqrt", (Object[] params) -> new Object[]{Math.sqrt(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.sqrt()", 1, runtime))});
    math.putFunction("tan", (Object[] params) -> new Object[]{Math.tan(LuaRuntime.assertDouble(params.length > 0 ? params[0] : null, "math.tan()", 1, runtime))});
    math.putFunction("tointeger", (Object[] params) -> {
      Object param0 = params.length > 1 ? params[0] : null;
      if (param0 instanceof Long) return new Object[]{param0};
      if (param0 instanceof Number n) return new Object[]{n.longValue()};
      if (param0 instanceof String s) {
        Number n = LuaRuntime.tonumber_raw(s);
        if (n instanceof Long l) return new Object[]{l};
        if (n instanceof Double d && !Double.isNaN(d)) return new Object[]{d};
        return new Object[]{null};
      }
      return new Object[]{null};
    });
    math.putFunction("type", (Object[] params) -> {
      Object param0 = params.length > 1 ? params[0] : null;
      if (param0 instanceof Long) return new Object[]{"integer"};
      if (param0 instanceof Double) return new Object[]{"float"};
      return new Object[]{null};
    });
    math.putFunction("ult", (Object[] params) -> {
      long m = LuaRuntime.assertNumber(params.length > 0 ? params[0] : null, "math.ult()", 1, runtime).longValue();
      long n = LuaRuntime.assertNumber(params.length > 1 ? params[1] : null, "math.ult()", 2, runtime).longValue();
      return new Object[]{Long.compareUnsigned(m, n) < 0};
    });

    this.put("math", math);
    return math;
  }

  public LuaTable addBit32Module() {
    LuaTable bit32 = new LuaTable();

    bit32.putFunction("arshift", (Object[] params) -> {
      int x = (int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.arshift()", 1, runtime);
      int disp = (int)LuaRuntime.assertInteger(params.length > 1 ? params[1] : null, "bit32.arshift()", 2, runtime);
      return new Object[]{(double)(x >> disp)};
    });

    bit32.putFunction("band", (Object[] params) -> {
      int result = 0xffffffff;
      if (params.length == 0) throw new LuaRuntimeError("bit32.band() needs at least 1 argument");
      for (int i = 0, n = params.length; i < n; i++) {
        Object param = params[i];
        int x = (int)LuaRuntime.assertInteger(param, "bit32.band()", i + 1, runtime);
        result &= x;
      }
      return new Object[]{(double)result};
    });

    bit32.putFunction("bnot", (Object[] params) -> new Object[]{(double)~(int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.bnot()", 1, runtime)});

    bit32.putFunction("bor", (Object[] params) -> {
      int result = 0x00000000;
      if (params.length == 0) throw new LuaRuntimeError("bit32.bor() needs at least 1 argument");
      for (int i = 0, n = params.length; i < n; i++) {
        Object param = params[i];
        int x = (int)LuaRuntime.assertInteger(param, "bit32.bor()", i + 1, runtime);
        result |= x;
      }
      return new Object[]{(double)result};
    });

    bit32.putFunction("btest", (Object[] params) -> {
      int result = 0xffffffff;
      if (params.length == 0) throw new LuaRuntimeError("bit32.btest() needs at least 1 argument");
      for (int i = 0, n = params.length; i < n; i++) {
        Object param = params[i];
        int x = (int)LuaRuntime.assertInteger(param, "bit32.btest()", i + 1, runtime);
        result &= x;
      }
      return new Object[]{result != 0};
    });

    bit32.putFunction("bxor", (Object[] params) -> {
      int result = 0x00000000;
      if (params.length == 0) throw new LuaRuntimeError("bit32.bxor() needs at least 1 argument");
      for (int i = 0, n = params.length; i < n; i++) {
        Object param = params[i];
        int x = (int)LuaRuntime.assertInteger(param, "bit32.bxor()", i + 1, runtime);
        result ^= x;
      }
      return new Object[]{(double)result};
    });

    bit32.putFunction("extract", (Object[] params) -> {
      int n = (int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.extract()", 1, runtime);
      int field = (int)LuaRuntime.assertInteger(params.length > 1 ? params[1] : null, "bit32.extract()", 2, runtime);
      int width = (int)LuaRuntime.assertInteger(Objects.requireNonNullElse(params.length > 2 ? params[2] : null, 31), "bit32.extract()", 3, runtime);
      return new Object[]{(double)(n & (0xffffffff << field) & ~(int)(0xfffffff8 << width))};
    });

    bit32.putFunction("replace", (Object[] params) -> {
      int n = (int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.replace()", 1, runtime);
      int v = (int)LuaRuntime.assertInteger(params.length > 1 ? params[1] : null, "bit32.replace()", 2, runtime);
      int field = (int)LuaRuntime.assertInteger(params.length > 2 ? params[2] : null, "bit32.replace()", 3, runtime);
      int width = (int)LuaRuntime.assertInteger(Objects.requireNonNullElse(params.length > 3 ? params[3] : null, 31), "bit32.replace()", 4, runtime);
      int mask = ~(int)(0xfffffff8 << width);
      return new Object[]{(double)(n & (0xffffffff << field) & ~mask | (v << field) & mask)};
    });

    bit32.putFunction("lrotate", (Object[] params) -> {
      int x = (int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.lrotate()", 1, runtime);
      int disp = (int)LuaRuntime.assertInteger(params.length > 1 ? params[1] : null, "bit32.lrotate()", 2, runtime);
      return new Object[]{(double)Integer.rotateLeft(x, disp)};
    });

    bit32.putFunction("lshift", (Object[] params) -> {
      int x = (int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.lshift()", 1, runtime);
      int disp = (int)LuaRuntime.assertInteger(params.length > 1 ? params[1] : null, "bit32.lshift()", 2, runtime);
      return new Object[]{(double)(x << disp)};
    });

    bit32.putFunction("rrotate", (Object[] params) -> {
      int x = (int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.rrotate()", 1, runtime);
      int disp = (int)LuaRuntime.assertInteger(params.length > 1 ? params[1] : null, "bit32.rrotate()", 2, runtime);
      return new Object[]{(double)Integer.rotateRight(x, disp)};
    });

    bit32.putFunction("rshift", (Object[] params) -> {
      int x = (int)LuaRuntime.assertInteger(params.length > 0 ? params[0] : null, "bit32.rshift()", 1, runtime);
      int disp = (int)LuaRuntime.assertInteger(params.length > 1 ? params[1] : null, "bit32.rshift()", 2, runtime);
      return new Object[]{(double)(x >> disp)};
    });

    this.put("bit32", bit32);
    return bit32;
  }

  public LuaTable addUTF8Module() {
    LuaTable utf8 = new LuaTable();

    this.put("utf8", utf8);
    return utf8;
  }

  public LuaTable addTableModule() {
    LuaTable table = new LuaTable();

//    table.putFunction("concat", (Object[] params) -> {
//      Object maybeList = params.length > 0 ? params[0] : null;
//      if (!(maybeList instanceof LuaTable)) throw new LuaRuntimeError("table.concat() expected a table for argument 1");
//    });
//    table.putFunction("create", (Object[] params) -> new Object[]{new LuaTable()});
//    table.putFunction("insert", (Object[] params) -> {});
//    table.putFunction("move", (Object[] params) -> { // new
//    });
//    table.putFunction("pack", (Object[] params) -> new Object[]{LuaTable.ofListN(params)});
//    table.putFunction("remove", (Object[] params) -> {});
//    table.putFunction("sort", (Object[] params) -> {});
//    table.putFunction("unpack", (Object[] params) -> {});

    this.put("table", table);
    return table;
  }

  public LuaTable addStringModule(boolean applyMetatable) {
    LuaTable string = new LuaTable();
    try {

      string.putFunction("byte", Bridge.methodToLua(StringLib.class.getDeclaredMethod("byte_", String.class, Optional.class, Optional.class)));
      string.putFunction("char", objects -> new Object[]{
          new String(
              Arrays.stream(objects)
                  .mapToInt(obj -> switch (obj) {
                    case Double d when Double.isFinite(d) && Math.floor(d) == d -> (int)(long)(double)d;
                    case Long l -> (int)(long)l;
                    default -> throw new LuaRuntimeError("string.char() expects integers only");
                  }).toArray()
          , 0, objects.length)
      });
      string.putFunction("dump", _ -> {throw new LuaRuntimeError("string.dump() is not supported on this runtime");});
      string.putFunction("len", Bridge.methodToLua(StringLib.class.getDeclaredMethod("len", String.class)));
      string.putFunction("lower", Bridge.methodToLua(StringLib.class.getDeclaredMethod("lower", String.class)));
      string.putFunction("rep", Bridge.methodToLua(StringLib.class.getDeclaredMethod("rep", String.class, int.class, Optional.class)));
      string.putFunction("reverse", Bridge.methodToLua(StringLib.class.getDeclaredMethod("reverse", String.class)));
      string.putFunction("sub", Bridge.methodToLua(StringLib.class.getDeclaredMethod("sub", String.class, int.class, Optional.class)));
      string.putFunction("upper", Bridge.methodToLua(StringLib.class.getDeclaredMethod("upper", String.class)));

    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    this.put("string", string);
    if (applyMetatable) {
      this.runtime.p.primitiveMetatableString.put("__index", string.copy().readonly());
    }
    return string;
  }

}
