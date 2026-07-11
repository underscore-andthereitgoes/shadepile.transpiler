package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class FunctionCall extends Expression implements StatementLike {
  public final Expression target;
  public final @Nullable String method;
  public final Expression[] arguments;

  public FunctionCall(Expression target, @Nullable String method, Expression[] arguments) {
    this.target = target;
    this.method = method;
    this.arguments = arguments;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".f.fcall(");
    if (this.method != null) {
      builder.append(CodeEmitter.runtimeParameterName);
      builder.append(".p.refBind(");
      this.target.emit(builder);
      builder.append(",\"");
      builder.append(StringEscapeUtils.escapeJava(this.method));
      builder.append("\")");
    } else {
      this.target.emit(builder);
    }
    for (Expression argument: this.arguments) {
      builder.append(',');
      argument.emit(builder);
    }
    builder.append(')');
  }

  @Override
  public boolean requiresSingle() {
    return true;
  }
}
