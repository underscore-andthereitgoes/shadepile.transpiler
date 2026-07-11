package underscore.andthereitgoes.shadepile.transpiler.lua.runtime;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


/// Helper functions for converting Java methods into Lua functions.
public final class Bridge {

  /**
   * Ports a Java method to a Lua function. Automatically detects parameter types.
   * <p>
   * <br>
   * Accepted parameter types:<ul>
   * <li>{@link Optional}: optional parameter (only filled if necessary to fit the parameter count, and takes precedence over {@code @}{@link Nillable})
   * <li>{@code @}{@link Nillable}: nil becomes null instead of an error
   * <li>{@code double}/{@link Double}: {@code number} (converted into a float)
   * <li>{@code long}/{@link Long}: {@code number} (converted into an integer)
   * <li>{@code boolean}/{@link Boolean}: {@code boolean}
   * <li>Exactly {@link Number}: {@code number} ({@link Double} or {@link Long})
   * <li>Any type that can have {@link String} assigned: {@code string}
   * <li>Exactly {@link Function}: {@code function} (<strong>should be {@code <Object[],Object[]>}, not checked</strong>)
   * <li>Any type that can have {@link List} assigned: {@code table} or {@code userdata}
   * <li>Any type that can have {@link Map} assigned: {@code table}
   * <li>Exactly {@link Object}: any type
   * <li>Exactly {@link Object}{@code []}: rest arguments (last parameter only)
   * <li>Any type that implements {@link LuaTableOrUserdata}: specific userdata
   * <br><em>If the type has a public static {@link String} field named {@code LUA_TYPE}, this will be used in the error instead of the class's simple name.</em>
   * </ul>
   * Other types will throw an error.<br>
   * Note: After {@link Optional}, if a parameter is directly assignable, it is passed straight through.
   * <p>
   * <br>
   * Runtime return type conversion:<ul>
   * <li>{@code double}/{@link Double}/{@code float}/{@link Float}: {@code number} (float)
   * <li>{@code long}/{@link Long}/{@code int}/{@link Integer}/{@code short}/{@link Short}: {@code number} (integer)
   * <li>{@code boolean}/{@link Boolean}: {@code boolean}
   * <li>{@code null}: {@code nil}
   * <li>{@code void}/{@link Void}: zero-value multires
   * <li>{@link String}: {@code string}
   * <li>{@link Function}: {@code function} (<strong>should be {@code <Object[],Object[]>}, not checked</strong>)
   * <li>{@link List}: {@code table} (collected into a {@link LuaTable})
   * <li>{@link LuaTableOrUserdata} (except {@link LuaTable}): left as-is
   * <li>{@link Map} (except {@link LuaTableOrUserdata}, but including {@link LuaTable}): {@code table}
   * <li>{@link Object}{@code []}, {@code int[]}, {@code long[]}, {@code double[]}: multires (conversion applied to each argument)
   * </ul>
   */
  public static @NotNull Function<Object[],Object[]> methodToLua(@NotNull Method function) {

    if (!Modifier.isStatic(function.getModifiers())) throw new IllegalArgumentException("methodToLua can only be applied to static methods");

    int pCount = function.getParameterCount();

    Class<?>[] pClasses = function.getParameterTypes();
    int[] pOptionals = Arrays.stream(pClasses).mapToInt(cls -> cls == Optional.class ? 1 : 0).toArray();
    Annotation[][] pAnnotations = function.getParameterAnnotations();
    int[] pNonNull = Arrays.stream(pAnnotations)
        .mapToInt(annotations ->
            Arrays.stream(annotations)
                .map(Annotation::annotationType)
                .anyMatch(cls -> cls == Nillable.class)
            ? 0 : 1
        ).toArray();

    int pCountRequired = (int)Arrays.stream(pClasses).filter(cls -> cls != Optional.class).count();

    Class<?> rType = function.getReturnType();
    Function<Object, Object[]> returnMapper = (rType == Void.class) ? (r -> new Object[0]) : (Bridge::autoconvertReturn);

    Function<@Nullable Object[], @Nullable Object[]> parameterMapper = objects -> {
      final boolean[] provideWhich = new boolean[pCount];
      final Object[] arguments = new Object[pCount];
      final int numProvided = objects.length;
      int available = numProvided;
      for (int i = 0; i < pCount; i++) {
        if (pOptionals[i] == 0) {
          if (available <= 0) throw new LuaRuntimeError("not enough arguments (need at least " + pCountRequired + ", got " + numProvided + ")");
          provideWhich[i] = true;
          available--;
        }
      }
      for (int i = 0; i < pCount && available > 0; i++) {
        if (!provideWhich[i]) {
          provideWhich[i] = true;
          available--;
        }
      }
      int j = 0;
      for (int i = 0; i < pCount; i++) {
        if (provideWhich[i]) {
          if (pClasses[i] == Object[].class) {
            if (i != pCount - 1) throw new IllegalArgumentException("(Java issue) cannot have rest parameter before last argument in Lua-ported function");
            arguments[i] = Arrays.copyOfRange(objects, j, pCount);
            break;
          } else {
            arguments[i] = mapArgumentByExpectedType(function, i, objects[j++], pClasses[i], pNonNull[i] != 0);
          }
        } else {
          arguments[i] = Optional.empty();
        }
      }
      return arguments;
    };

    return params -> {
      try {
        return returnMapper.apply(function.invoke(null, parameterMapper.apply(params)));
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private static @Nullable Object mapArgumentByExpectedType(Method method, int argumentIndex0b, final @Nullable Object argument, Class<?> expectType, boolean requireNonNull) {
    boolean wrapInOptional = false;
    if (expectType == Optional.class) {
      wrapInOptional = true;
      expectType = expectType.getTypeParameters()[0].getClass();
    }
    Object assignArgument = null;
    boolean nillable = !expectType.isPrimitive() && !requireNonNull;
    if (argument == null && wrapInOptional) {
      assignArgument = Optional.empty();
    } else if (argument == null && nillable) {
      return null;
    } else {
      if (argument != null && expectType.isAssignableFrom(argument.getClass())) {
        assignArgument = argument;
      } else {
        // this block only runs if argument is a different type to expectType
        String luaTypeName = expectType.getSimpleName();
        if (luaTypeName.isEmpty()) luaTypeName = "???";
        if (expectType == boolean.class || expectType == Boolean.class) {
          assignArgument = argument != null;
          luaTypeName = "boolean";
        } else if (expectType == short.class || expectType == Short.class) {
          assignArgument = switch (argument) {
            case Long l -> (short)(long)l;
            case Double d when Double.isFinite(d) && d == Math.floor(d) -> (short)(double)d;
            case null, default -> null;
          };
          luaTypeName = "integral number";
        } else if (expectType == int.class || expectType == Integer.class) {
          assignArgument = switch (argument) {
            case Long l -> (int)(long)l;
            case Double d when Double.isFinite(d) && d == Math.floor(d) -> (int)(double)d;
            case null, default -> null;
          };
          luaTypeName = "integral number";
        } else if (expectType == long.class || expectType == Long.class) {
          assignArgument = switch (argument) {
            case Long l -> l;
            case Double d when Double.isFinite(d) && d == Math.floor(d) -> (long)(double)d;
            case null, default -> null;
          };
          luaTypeName = "integral number";
        } else if (expectType == float.class || expectType == Float.class) {
          assignArgument = switch (argument) {
            case Long l -> (float)(long)l;
            case Double d -> (float)(double)d;
            case null, default -> null;
          };
          luaTypeName = "number";
        } else if (expectType == double.class || expectType == Double.class) {
          assignArgument = switch (argument) {
            case Long l -> (double)(long)l;
            case Double d -> d;
            case null, default -> null;
          };
          luaTypeName = "number";
        } else if (expectType == Number.class) {
          assignArgument = switch (argument) {
            case Long l -> l;
            case Double d -> d;
            case null, default -> null;
          };
          luaTypeName = "number";
        } else if (expectType.isAssignableFrom(List.class)) {
          if (argument instanceof LuaTable table) {
            assignArgument = table.listValues();
          }
          luaTypeName = "table (list)";
        } else if (expectType.isAssignableFrom(String.class)) {
          luaTypeName = "string";
        } else if (expectType.isAssignableFrom(Function.class)) {
          luaTypeName = "function";
        } else if (expectType.isAssignableFrom(Map.class)) {
          luaTypeName = "table";
        } else if (expectType == Object.class) {
          luaTypeName = "any";
        } else if (LuaTableOrUserdata.class.isAssignableFrom(expectType)) {
          luaTypeName = expectType.getSimpleName();
          try {
            luaTypeName = expectType.getDeclaredField("LUA_TYPE").get(null).toString();
          } catch (Exception ignored) {}
        } else throw new IllegalArgumentException("(Java issue) cannot require parameter type " + expectType.getName() + " in method " + method.toString() + " (did you forget to implement LuaTableOrUserdata?)");
        if (assignArgument == null) {
          if (wrapInOptional) luaTypeName = "optional " + luaTypeName;
          else if (nillable) luaTypeName = luaTypeName + " or nil";
          String msg = "expected " + luaTypeName + " for argument " + (argumentIndex0b + 1);
          throw new LuaRuntimeError(msg);
        }
      }
      if (wrapInOptional) {
        assignArgument = Optional.of(assignArgument);
      }
    }
    return assignArgument;
  }

  public static boolean single(boolean any) { return any; }
  public static double single(double any) { return any; }
  public static long single(long any) { return any; }
  public static String single(String any) { return any; }
  public static @Nullable Object single(@Nullable Object any) {
    return any instanceof Object[] multi ? (multi.length > 0 ? multi[0] : null) : any;
  }

  private static long autoconvert(short value) { return value; }
  private static long autoconvert(int value) { return value; }
  private static long autoconvert(long value) { return value; }
  private static double autoconvert(float value) { return value; }
  private static double autoconvert(double value) { return value; }
  private static boolean autoconvert(boolean value) { return value; }
  private static double autoconvert(@NotNull Double value) { return value; }
  private static double autoconvert(@NotNull Float value) { return value; }
  private static long autoconvert(@NotNull Long value) { return value; }
  private static long autoconvert(@NotNull Integer value) { return value; }
  private static long autoconvert(@NotNull Short value) { return value; }
  private static boolean autoconvert(@NotNull Boolean value) { return value; }
  private static @NotNull String autoconvert(@NotNull String value) { return value; }
  private static @Nullable Object autoconvert(@Nullable Object value) {
    return switch (value) {
      case Double d -> autoconvert(d);
      case Float f -> autoconvert(f);
      case Long l -> autoconvert(l);
      case Integer i -> autoconvert(i);
      case Short s -> autoconvert(s);
      case Boolean b -> autoconvert(b);
      case String s -> autoconvert(s);
      case int[] array -> Arrays.stream(array).mapToLong(Bridge::autoconvert).toArray();
      case long[] array -> array;
      case double[] array -> array;
      case Object[] array -> Arrays.stream(array).map(Bridge::autoconvert).toArray(Object[]::new);
      case Function<?,?> f -> f;
      case List<?> list -> LuaTable.ofList(list.stream().map(Bridge::autoconvert).map(Bridge::single).toArray());
      case LuaTableOrUserdata _ -> value;
      case Map<?,?> map -> {
        LuaTable t = new LuaTable();
        map.forEach((k, v) -> t.put(single(autoconvert(k)), single(autoconvert(v))));
        yield t;
      }
      case null -> null;
      default -> throw new IllegalArgumentException("(Java issue) cannot return type " + value.getClass().getName() + " from a Lua-ported function (did you forget to implement LuaTableOrUserdata?)");
    };
  }

  private static Object[] autoconvertReturn(Object value) {
    if (value instanceof Object[] array) return array;
    else return new Object[]{value};
  }

  /// Marks a parameter as required (not nil) when the method is ported into a Lua function.
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Nillable {}
}
