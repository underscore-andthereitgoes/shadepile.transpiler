package underscore.andthereitgoes.shadepile.transpiler.lua;

import java.util.Arrays;


public enum KeywordTokenType {
  FOR("for"),
  DO("do"),
  END("end"),
  IF("if"),
  THEN("then"),
  ELSEIF("elseif"),
  ELSE("else"),
  WHILE("while"),
  REPEAT("repeat"),
  UNTIL("until"),
  FUNCTION("function"),
  RETURN("return"),
  BREAK("break"),
  GOTO("goto"),
  LOCAL("local"),
  GLOBAL("global"),
  ;

  public final String text;
  KeywordTokenType(String text) {
    this.text = text;
  }

  public static final KeywordTokenType[] descendingLength;
  static {
    Arrays.sort(descendingLength = values(), (s1, s2) -> Integer.compare(s2.text.length(), s1.text.length()));
  }

}
