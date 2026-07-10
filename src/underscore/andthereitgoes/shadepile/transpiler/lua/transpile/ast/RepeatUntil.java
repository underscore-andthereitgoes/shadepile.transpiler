package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.Set;


public class RepeatUntil extends Statement implements HasChildBlock {

  public final Block body;
  public final Expression condition;

  public RepeatUntil(Expression condition, Block body, boolean bodyDirect) {
    this.condition = condition;
    this.body = bodyDirect ? body : new Block(body, true).enableBreak();
  }

  public RepeatUntil(Expression condition, Block parent) {
    this(condition, parent, false);
  }

  @Override
  public Set<Block> getChildBlocks() {
    return Set.of(this.body);
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append("if(true)while(true){");
    this.body.emitWithoutBrackets(builder);
    builder.append("if(");
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".o.asbool(");
    this.condition.emit(builder);
    builder.append("))break;}");
  }
}
