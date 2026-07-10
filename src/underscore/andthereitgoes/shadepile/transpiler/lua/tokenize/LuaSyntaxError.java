package underscore.andthereitgoes.shadepile.transpiler.lua.tokenize;

import org.jetbrains.annotations.Contract;


public class LuaSyntaxError extends RuntimeException {
  public int line = 0;
  public int column = 0;
  public LuaSyntaxError(String message) {
    super(message);
  }
  @Contract("_,_ -> this")
  public LuaSyntaxError at(int line, int column) {
    this.line = line;
    this.column = column;
    return this;
  }
}
