package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class GroupingExpression extends Expression {

  public final Expression expression;

  public GroupingExpression(Expression expression) {
    this.expression = expression;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    expression.emitSingle(builder);
  }
}
