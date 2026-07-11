package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.LuaCompileError;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.Objects;


public class VariableReference extends VariableOrPropertyAccess {

  public static final String ENV_NAME = CodeEmitter.environmentParameterName;

  public final @Nullable String localName;
  public final @Nullable Block block;
  public int scopeIndex;

  @Contract(value = "null,!null -> fail; !null,null -> fail")
  private VariableReference(@Nullable String localName, @Nullable Block block) {
    this(localName, block, -1);
  }

  @Contract(value = "null,!null,_ -> fail; !null,null,_ -> fail")
  private VariableReference(@Nullable String localName, @Nullable Block block, int scopeIndex) {
    if ((localName == null) != (block == null)) throw new IllegalArgumentException();
    if (localName != null && !localName.matches("^([A-Za-z_][A-Za-z_0-9]*|\\.\\.\\.)$")) throw new LuaCompileError("bad variable name");
    this.localName = localName;
    this.block = block;
    this.scopeIndex = scopeIndex;
  }

  @Contract(value = "-> new", pure = true)
  public static VariableReference env() {
    return new VariableReference(null, null, 0);
  }

  @Contract(value = "_,_,_ -> new", pure = true)
  public static VariableReference local(@NotNull String name, @NotNull Block block, int scopeIndex) {
    return new VariableReference(name, block, scopeIndex);
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    if (this.block == null) {
      builder.append(ENV_NAME);
    } else {
      if (this.scopeIndex < 0) throw new IllegalStateException("undeclared local variable used");
      builder.append("scope$");
      builder.append(this.block.getGroupName());
      builder.append("[");
      builder.append(String.valueOf(this.scopeIndex));
      builder.append("]");
    }
  }

  @Override
  public boolean requiresSingle() {
    return Objects.equals(this.localName, "...");
  }
}
