package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.Set;


public class ForGeneric extends For {

  public static final String javaIteratorName = "$gfor";

  public final String[] iteratorNames;
  public final Expression[] iteratorExpressions;
  public final Block body;

  public ForGeneric(String[] iteratorNames, Expression[] iteratorExpressions, Block parent) {
    this.iteratorNames = iteratorNames;
    this.iteratorExpressions = iteratorExpressions;
    this.body = new Block(parent, true).breakable();
    this.predefineIterators();
  }

  private void predefineIterators() {
    String[] names = this.iteratorNames;
    for (int i = 0, namesLength = names.length; i < namesLength; i++) {
      String iteratorName = names[i];
      this.body.predefineLocal(iteratorName, javaIteratorName + ".length>" + i + "?" + javaIteratorName + "[" + i + "]:null");
    }
  }

  @Override
  public Set<Block> getChildBlocks() {
    return Set.of(this.body);
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    this.body.appendBreakLabelIfRequired(builder);
    builder.append("for(Object[] ");
    builder.append(javaIteratorName);
    builder.append(':');
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".f.iterator(");
    if (this.iteratorExpressions.length == 0) builder.append("null");
    else if (this.iteratorExpressions.length == 1) {
      this.iteratorExpressions[0].emit(builder);
    } else {
      builder.append(CodeEmitter.runtimeParameterName);
      builder.append(".u.multires(");
      Expression[] expressions = this.iteratorExpressions;
      for (int i = 0, expressionsLength = expressions.length; i < expressionsLength; i++) {
        if (i != 0) builder.append(',');
        expressions[i].emit(builder);
      }
      builder.append(')');
    }
    builder.append("))");
    this.body.emit(builder);
  }
}
