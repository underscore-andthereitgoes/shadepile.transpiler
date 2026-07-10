package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.apache.commons.text.StringEscapeUtils;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public abstract class Literal<T> extends Expression {
  public final T value;
  protected Literal(T value) {
    this.value = value;
  }

  public static class StringLiteral extends Literal<String> {
    public StringLiteral(String value) {
      super(value);
    }

    @Override
    public void emit(NewlineCountingStringBuilder builder) {
      builder.append('"');
      builder.append(StringEscapeUtils.escapeJava(value));
      builder.append('"');
    }
  }

  public static class IntegerLiteral extends Literal<Long> {
    public IntegerLiteral(long value) {
      super(value);
    }

    @Override
    public void emit(NewlineCountingStringBuilder builder) {
      builder.append(String.valueOf((long)value));
      builder.append('L');
    }
  }

  public static class FloatLiteral extends Literal<Double> {
    public FloatLiteral(double value) {
      super(value);
    }

    @Override
    public void emit(NewlineCountingStringBuilder builder) {
      builder.append(String.valueOf((double)value));
      builder.append('d');
    }
  }

  public static class BooleanLiteral extends Literal<Boolean> {
    public BooleanLiteral(boolean value) {
      super(value);
    }

    @Override
    public void emit(NewlineCountingStringBuilder builder) {
      builder.append(value.toString());
    }
  }

  public static class NilLiteral extends Literal<Void> {
    public NilLiteral() {
      super(null);
    }

    @Override
    public void emit(NewlineCountingStringBuilder builder) {
      builder.append("null");
    }
  }
}
