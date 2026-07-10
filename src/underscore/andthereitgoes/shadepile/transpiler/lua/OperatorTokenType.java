package underscore.andthereitgoes.shadepile.transpiler.lua;

import java.util.Arrays;


public enum OperatorTokenType {
  AND("and", null, BinaryOperator.AND, true),
  OR("or", null, BinaryOperator.OR, true),
  NOT("not", UnaryOperator.NOT, null),
  DOUBLE_DOT("..", null, BinaryOperator.CONCAT),
  HASH("#", UnaryOperator.LEN, null),
  LT("<", null, BinaryOperator.LT),
  GT(">",  null, BinaryOperator.GT),
  LT_EQ("<=", null, BinaryOperator.LE),
  GT_EQ(">=", null, BinaryOperator.GE),
  EQ("==", null, BinaryOperator.EQ),
  TILDE_EQ("~=", null, BinaryOperator.NE),
  AMP("&", null, BinaryOperator.BAND),
  PIPE("|", null, BinaryOperator.BOR),
  TILDE("~", UnaryOperator.BNOT, BinaryOperator.BXOR),
  LT_LT("<<",  null, BinaryOperator.SHL),
  GT_GT(">>",  null, BinaryOperator.SHR),
  PLUS("+", null, BinaryOperator.ADD),
  MINUS("-", UnaryOperator.UNM, BinaryOperator.SUB),
  ASTERISK("*", null, BinaryOperator.MUL),
  SLASH("/", null, BinaryOperator.DIV),
  DOUBLE_SLASH("//", null, BinaryOperator.IDIV),
  PERCENT("%", null, BinaryOperator.MOD),
  ;

  public final String text;
  public final UnaryOperator unaryEquivalent;
  public final BinaryOperator binaryEquivalent;
  public final boolean secondArgumentSupplied;
  OperatorTokenType(String text, UnaryOperator u, BinaryOperator b) {
    this.text = text;
    this.unaryEquivalent = u;
    this.binaryEquivalent = b;
    this.secondArgumentSupplied = false;
  }
  OperatorTokenType(String text, UnaryOperator u, BinaryOperator b, boolean secondArgumentSupplied) {
    this.text = text;
    this.unaryEquivalent = u;
    this.binaryEquivalent = b;
    this.secondArgumentSupplied = secondArgumentSupplied;
  }

  public static final OperatorTokenType[] descendingLength;
  static {
    Arrays.sort(descendingLength = values(), (s1, s2) -> Integer.compare(s2.text.length(), s1.text.length()));
  }

}
