package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class Break extends Statement {
  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append("if(true)break;");
  }
}
