package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class PropertyAccess extends VariableOrPropertyAccess {

  public final Expression target;
  public final Expression property;

  public PropertyAccess(Expression target, Expression property) {
    this.target = target;
    this.property = property;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".p.get(");
    this.emitRef(builder);
    builder.append(')');
  }

  public void emitRef(NewlineCountingStringBuilder builder) {
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".p.ref(");
    this.target.emit(builder);
    builder.append(',');
    this.property.emit(builder);
    builder.append(')');
  }
}
