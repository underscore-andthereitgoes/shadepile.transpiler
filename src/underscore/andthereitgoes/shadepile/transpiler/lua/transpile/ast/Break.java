package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class Break extends Statement {

  private String breakGroup = null;

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    if (this.breakGroup == null) throw new IllegalStateException();
    builder.append("if(true)break ");
    builder.append(this.breakGroup);
    builder.append(";");
  }

  public void setBreakGroup(String groupName) {
    this.breakGroup = groupName;
  }
}
