package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.ArrayList;


public class TableConstructor extends Expression {

  public static final Expression noKeyExpression = new Expression() {
    @Override
    public void emit(NewlineCountingStringBuilder builder) {
      builder.append(CodeEmitter.runtimeParameterName);
      builder.append(".noKey");
    }
  };

  private final ArrayList<Expression> flatEntries = new ArrayList<>();

  public TableConstructor() {
  }

  public void addEntry(Expression value) {
    addEntry(noKeyExpression, value);
  }

  public void addEntry(Expression key, Expression value) {
    this.flatEntries.add(key);
    this.flatEntries.add(value);
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".u.table(");
    ArrayList<Expression> entries = this.flatEntries;
    for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
      if (i != 0) builder.append(',');
      entries.get(i).emit(builder);
    }
    builder.append(')');
  }
}
