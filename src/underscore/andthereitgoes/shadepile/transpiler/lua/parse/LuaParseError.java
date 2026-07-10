package underscore.andthereitgoes.shadepile.transpiler.lua.parse;

import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;


public class LuaParseError extends RuntimeException {
  public Token at = null;
  public LuaParseError(String message) {
    super(message);
  }
}
