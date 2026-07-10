package underscore.andthereitgoes.shadepile.transpiler.lua;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.UnaryOperatorOrExpression;


public enum UnaryOperator implements UnaryOperatorOrExpression {
  NOT("not"),
  LEN("len"),
  UNM("unm"),
  BNOT("bnot"),
  ;

  public final String name;
  UnaryOperator(String name) {
    this.name = name;
  }
}
