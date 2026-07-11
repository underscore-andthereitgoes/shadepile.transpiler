package underscore.andthereitgoes.shadepile.transpiler.lua.tokenize;

import underscore.andthereitgoes.shadepile.transpiler.lua.KeywordTokenType;
import underscore.andthereitgoes.shadepile.transpiler.lua.OperatorTokenType;


public abstract class Token {
  public int line;
  public int column;

  public enum BracketSide { LEFT, RIGHT }

  private abstract static class Literal <T> extends Token {
    public final T value;
    private Literal(T value) {
      this.value = value;
    }
  }
  public static class StringLiteral extends Literal<String> {
    public StringLiteral(String value) { super(value); }
  }
  public static class NumberLiteral extends Literal<Number> {
    public NumberLiteral(long value) { super(value); }
    public NumberLiteral(double value) { super(value); }
  }
  public static class BooleanLiteral extends Literal<Boolean> {
    public BooleanLiteral(boolean value) { super(value); }
  }
  public static class NilLiteral extends Literal<Void> {
    public NilLiteral() { super(null); }
  }

  public static class Name extends Token {
    public final String text;
    public Name(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return super.toString() + ":" + this.text;
    }
  }

  public static class LabelBoundary extends Token {}

  public static abstract class AnyBracket extends Token {
    public final Tokenizer.BracketType type;
    public final BracketSide side;
    public AnyBracket(BracketSide side, Tokenizer.BracketType type) {
      this.side = side;
      this.type = type;
    }
  }
  public static class Parenthesis extends AnyBracket {
    public Parenthesis(BracketSide side) {
      super(side, Tokenizer.BracketType.PAREN);
    }
  }
  public static class CurlyBracket extends AnyBracket {
    public CurlyBracket(BracketSide side) {
      super(side, Tokenizer.BracketType.CURLY);
    }
  }
  public static class SquareBracket extends AnyBracket {
    public SquareBracket(BracketSide side) {
      super(side, Tokenizer.BracketType.SQUARE);
    }
  }

  public static class Operator extends Token {
    public final OperatorTokenType type;
    public Operator(OperatorTokenType type) {
      this.type = type;
    }
  }

  public static abstract class AnySeparator extends Token {}
  public static class Semicolon extends AnySeparator {}
  public static class Comma extends AnySeparator {}

  public static class ThreeDots extends Token {}

  public static abstract class PropertyAccess extends Token {}
  public static class PropertyDot extends PropertyAccess {}
  public static class PropertyColon extends PropertyAccess {}

  public static class SingleEquals extends Token {}

  public static class Keyword extends Token {
    public final KeywordTokenType type;
    public Keyword(KeywordTokenType type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return super.toString() + ":" + this.type.toString();
    }
  }
}
