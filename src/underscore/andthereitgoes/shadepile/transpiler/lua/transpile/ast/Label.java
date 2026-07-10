package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.LuaCompileError;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class Label extends Statement implements BlockDirective {
  public final String name;
  public Block block;

  public Label(String name) {
    this.name = name;
  }

  @Override
  public void onPush(Block block) {
    this.block = block;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    if (!this.name.matches("^[A-Za-z_][A-Za-z_0-9]*$")) throw new LuaCompileError("bad label name"); // prevent ACE if label parsing wasn't implemented correctly
    builder.append("case \"");
    builder.append(this.name);
    builder.append("\":");
  }
}
