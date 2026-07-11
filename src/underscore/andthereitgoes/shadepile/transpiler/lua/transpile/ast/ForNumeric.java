package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.Set;


public class ForNumeric extends For {

  public static final String javaIteratorName = "$nfor";

  public final String iteratorName;
  public final Expression[] expressions;
  public final Block body;

  public ForNumeric(String iteratorName, Expression[] expressions, Block parent) {
    if (expressions == null || expressions.length != 2 && expressions.length != 3) throw new IllegalStateException("incorrect expressions for numeric for");
    this.iteratorName = iteratorName;
    this.expressions = expressions;
    this.body = new Block(parent, true).breakable();
    this.predefineIterator();
  }

  private void predefineIterator() {
    this.body.predefineLocal(this.iteratorName, javaIteratorName);
  }

  @Override
  public Set<Block> getChildBlocks() {
    return Set.of(this.body);
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    this.body.appendBreakLabelIfRequired(builder);
    builder.append("for(Object ");
    builder.append(javaIteratorName);
    builder.append(':');
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".f.numericIterator(");
    this.expressions[0].emit(builder);
    builder.append(',');
    this.expressions[1].emit(builder);
    if (this.expressions.length > 2) {
      builder.append(',');
      this.expressions[2].emit(builder);
    }
    builder.append("))");
    this.body.emit(builder);
  }
}
