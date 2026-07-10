package underscore.andthereitgoes.shadepile.transpiler.lua;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;


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
  public final @Nullable UnaryOperator unaryEquivalent;
  public final @Nullable BinaryOperator binaryEquivalent;
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

  public static final ParseOrderItem[] order = new ParseOrderItem[]{
      new ParseOrderItemBinary(BinaryOperator.POW, true),
      new ParseOrderItemUnary(UnaryOperator.BNOT, UnaryOperator.UNM, UnaryOperator.LEN, UnaryOperator.NOT),
      new ParseOrderItemBinary(BinaryOperator.MOD, BinaryOperator.IDIV, BinaryOperator.DIV, BinaryOperator.MUL),
      new ParseOrderItemBinary(BinaryOperator.SUB, BinaryOperator.ADD),
      new ParseOrderItemBinary(BinaryOperator.CONCAT, true),
      new ParseOrderItemBinary(BinaryOperator.SHR, BinaryOperator.SHL),
      new ParseOrderItemBinary(BinaryOperator.BAND),
      new ParseOrderItemBinary(BinaryOperator.BXOR),
      new ParseOrderItemBinary(BinaryOperator.BOR),
      new ParseOrderItemBinary(BinaryOperator.EQ, BinaryOperator.NE, BinaryOperator.GE, BinaryOperator.LE, BinaryOperator.GT, BinaryOperator.LT),
      new ParseOrderItemBinary(BinaryOperator.AND),
      new ParseOrderItemBinary(BinaryOperator.OR),
  };

  public static abstract sealed class ParseOrderItem {
    public final boolean reverse;

    protected ParseOrderItem(boolean reverse) {
      this.reverse = reverse;
    }
  }
  public static non-sealed class ParseOrderItemUnary extends ParseOrderItem {
    public final Set<UnaryOperator> operators;

    public ParseOrderItemUnary(UnaryOperator[] operators, boolean reverse) {
      super(reverse);
      this.operators = Set.of(operators);
    }
    public ParseOrderItemUnary(UnaryOperator... operators) {
      this(operators, false);
    }
    public ParseOrderItemUnary(UnaryOperator operator, boolean reverse) {
      this(new UnaryOperator[]{operator}, reverse);
    }
    public ParseOrderItemUnary(UnaryOperator operator) {
      this(operator, false);
    }
  }
  public static non-sealed class ParseOrderItemBinary extends ParseOrderItem {
    public final Set<BinaryOperator> operators;

    public ParseOrderItemBinary(BinaryOperator[] operators, boolean reverse) {
      super(reverse);
      this.operators = Set.of(operators);
    }
    public ParseOrderItemBinary(BinaryOperator... operators) {
      this(operators, false);
    }
    public ParseOrderItemBinary(BinaryOperator operator, boolean reverse) {
      this(new BinaryOperator[]{operator}, reverse);
    }
    public ParseOrderItemBinary(BinaryOperator operator) {
      this(operator, false);
    }
  }
}
