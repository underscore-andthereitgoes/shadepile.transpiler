package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.jetbrains.annotations.NotNull;
import underscore.andthereitgoes.shadepile.transpiler.lua.BinaryOperator;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class BinaryOperation extends Operation {

  public final @NotNull Expression leftOperand;
  public final @NotNull Expression rightOperand;
  public final @NotNull BinaryOperator operator;

  public BinaryOperation(@NotNull Expression leftOperand, @NotNull Expression rightOperand, @NotNull BinaryOperator operator) {
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
    this.operator = operator;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".o.");
    builder.append(this.operator.name);
    builder.append('(');
    this.leftOperand.emitNewlines(builder);
    this.leftOperand.emit(builder);
    builder.append(',');
    if (this.operator.supplySecond) builder.append("()->");
    this.rightOperand.emitNewlines(builder);
    this.rightOperand.emit(builder);
    builder.append(')');
  }
}
