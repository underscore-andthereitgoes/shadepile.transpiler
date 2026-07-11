package underscore.andthereitgoes.shadepile.transpiler.lua.parse;

import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;


public class LuaParseError extends RuntimeException {
  public Token at = null;
  public LuaParseError(String message) {
    super(message);
  }

  @Override
  public String getLocalizedMessage() {
    String msg = super.getLocalizedMessage();
    if (this.at != null) msg = msg + "\n        at " + this.at + "\n        (lua:" + this.at.line + ")";
    return msg;
  }
}
