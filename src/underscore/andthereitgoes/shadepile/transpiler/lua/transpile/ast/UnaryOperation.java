package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.jetbrains.annotations.NotNull;
import underscore.andthereitgoes.shadepile.transpiler.lua.UnaryOperator;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class UnaryOperation extends Operation {

  public final @NotNull UnaryOperator operator;
  public final @NotNull Expression operand;

  public UnaryOperation(@NotNull UnaryOperator operator, @NotNull Expression operand) {
    this.operator = operator;
    this.operand = operand;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".o.");
    builder.append(this.operator.name);
    builder.append('(');
    this.operand.emitNewlines(builder);
    this.operand.emit(builder);
    builder.append(')');
  }
}
