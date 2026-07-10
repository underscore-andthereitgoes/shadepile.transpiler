package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class Return extends Statement {

  public final Expression[] values;

  public Return(Expression[] values) {
    this.values = values;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append("if(true)return new Object[]{");
    Expression[] expressions = this.values;
    for (int i = 0, expressionsLength = expressions.length; i < expressionsLength; i++) {
      if (i != 0) builder.append(',');
      Expression expr = expressions[i];
      expr.emit(builder);
    }
    builder.append("};");
  }
}
