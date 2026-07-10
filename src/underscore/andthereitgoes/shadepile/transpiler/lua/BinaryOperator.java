package underscore.andthereitgoes.shadepile.transpiler.lua;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.OperatorOrExpression;


public enum BinaryOperator implements OperatorOrExpression {
  AND("and", true),
  OR("or", true),
  LT("lt"),
  GT("gt"),
  LE("le"),
  GE("ge"),
  EQ("eq"),
  NE("ne"),
  BOR("bor"),
  BXOR("bxor"),
  BAND("band"),
  SHL("shl"),
  SHR("shr"),
  CONCAT("concat"),
  ADD("add"),
  SUB("sub"),
  MUL("mul"),
  DIV("div"),
  IDIV("idiv"),
  MOD("mod"),
  POW("pow"),
  ;

  public final String name;
  public final boolean supplySecond;

  BinaryOperator(String name) {
    this(name, false);
  }
  BinaryOperator(String name, boolean supplySecond) {
    this.name = name;
    this.supplySecond = supplySecond;
  }
}
