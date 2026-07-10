package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.Set;


public class RepeatWhile extends Statement implements HasChildBlock {

  public final Expression condition;
  public final Block body;

  public RepeatWhile(Expression condition, Block parent) {
    this.condition = condition;
    this.body = new Block(parent, true).enableBreak();
  }

  @Override
  public Set<Block> getChildBlocks() {
    return Set.of(body);
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append("if(true)while(");
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".o.asbool(");
    this.condition.emit(builder);
    builder.append("))");
    this.body.emit(builder);
  }
}
