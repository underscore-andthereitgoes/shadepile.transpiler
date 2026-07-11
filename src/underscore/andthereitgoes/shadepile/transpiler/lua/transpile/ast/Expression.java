package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;


import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public abstract class Expression extends PositionedNode implements UnaryOperatorOrExpression {
  /// Returns `true` if this expression needs to be wrapped in a `single` call.
  public boolean requiresSingle() {
    return false;
  }

  /// Emits the `single` call prefix, if necessary, around a regular emit.
  public void emitSingle(NewlineCountingStringBuilder builder) {
    if (this.requiresSingle()) {
      builder.append(CodeEmitter.runtimeParameterName);
      builder.append(".u.single(");
      this.emit(builder);
      builder.append(')');
    } else {
      this.emit(builder);
    }
  }
}
