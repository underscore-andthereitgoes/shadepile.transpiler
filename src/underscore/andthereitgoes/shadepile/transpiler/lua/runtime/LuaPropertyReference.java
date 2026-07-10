package underscore.andthereitgoes.shadepile.transpiler.lua.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class LuaPropertyReference {
  public static final @NotNull Object theNowherePropertyValue = NoSuchFieldError.class;
  public final @Nullable LuaTableOrUserdata target;
  public final @NotNull Object property;
  public final boolean isNowhere;
  public boolean selfBinding;
  public LuaPropertyReference(@Nullable LuaTableOrUserdata target, @Nullable Object property) {
    this.target = target;
    this.property = property == null ? theNowherePropertyValue : property;
    this.isNowhere = target == null || property == null;
  }
  public static LuaPropertyReference nowhere() {
    return new LuaPropertyReference(null, null);
  }
}
