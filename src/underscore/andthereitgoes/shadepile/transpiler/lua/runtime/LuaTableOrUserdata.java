package underscore.andthereitgoes.shadepile.transpiler.lua.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HexFormat;
import java.util.Map;


public interface LuaTableOrUserdata extends Map<Object, Object> {
  /**
   * Note: May not be accurate. Returns 0 when the table is empty, and greater than zero when it is not. Other values are ambiguous.
   */
  @Override
  default int size() {
    return this.isEmpty() ? 0 : 1;
  }

  @Override
  default void putAll(@NotNull Map<?,?> m) {
    m.forEach(this::put);
  }

  /** The count (#) operator in Lua. */
  long len();

  @Nullable LuaTableOrUserdata getMetatable();
  void setMetatable(@Nullable LuaTableOrUserdata metatable);

  default String toLuaString() {
    return "userdata: " + HexFormat.of().toHexDigits(this.hashCode());
  }
}
