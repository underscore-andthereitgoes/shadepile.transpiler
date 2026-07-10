package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class Do extends Statement {
  public final Block body;

  public Do(Block parent) {
    this.body = new Block(parent, true);
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    this.body.emit(builder);
  }
}
