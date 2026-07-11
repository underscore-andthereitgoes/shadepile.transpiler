package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class Assignment extends Statement {
  public final VariableOrPropertyAccess[] variables;
  public final Expression[] values;

  public Assignment(VariableOrPropertyAccess[] variables, Expression[] values) {
    this.variables = variables;
    this.values = values;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    if (variables.length == 1) {
      VariableOrPropertyAccess variable = variables[0];
      switch (variable) {
        case VariableReference localVariable -> {
          localVariable.emit(builder);
          builder.append('=');
          if (values.length > 0) {
            values[0].emitNewlines(builder);
            values[0].emitSingle(builder);
          } else builder.append("null");
          builder.append(';');
        }
        case PropertyAccess propertyAccess -> {
          builder.append(CodeEmitter.runtimeParameterName);
          builder.append(".p.set(");
          propertyAccess.emitRef(builder);
          builder.append(',');
          if (values.length > 0) {
            values[0].emitNewlines(builder);
            values[0].emit(builder);
          } else builder.append("null");
          builder.append(");");
        }
        default -> throw new IllegalStateException();
      }
    } else {

      builder.append('{');

      int variablesLength = variables.length;
      for (int i = 0; i < variablesLength; i++) {
        VariableOrPropertyAccess variable = variables[i];
        if (variable instanceof PropertyAccess propertyAccess) {
          builder.append("LuaPropertyReference ref");
          builder.append(String.valueOf(i));
          builder.append('=');
          propertyAccess.emitRef(builder);
          builder.append(';');
        }
      }

      builder.append("Object[] values=");
      builder.append(CodeEmitter.runtimeParameterName);
      builder.append(".u.multires(");
      for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
        if (i != 0) builder.append(',');
        Expression value = values[i];
        value.emit(builder);
      }
      builder.append(");");

      for (int i = 0; i < variablesLength; i++) {
        VariableOrPropertyAccess variable = variables[i];

        boolean closeParen;
        if (variable instanceof VariableReference variableReference) {
          variableReference.emit(builder);
          builder.append('=');
          closeParen = false;
        } else if (variable instanceof PropertyAccess) {
          builder.append(CodeEmitter.runtimeParameterName);
          builder.append(".p.set(ref");
          builder.append(String.valueOf(i));
          builder.append(',');
          closeParen = true;
        } else throw new IllegalStateException();

        builder.append("values.length>");
        builder.append(String.valueOf(i));
        builder.append("?values[");
        builder.append(String.valueOf(i));
        builder.append("]:null");

        if (closeParen) builder.append(')');
        builder.append(';');

      }

      builder.append('}');

    }
  }
}
