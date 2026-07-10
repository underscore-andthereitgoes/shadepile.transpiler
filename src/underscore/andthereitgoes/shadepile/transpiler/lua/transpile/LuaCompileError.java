package underscore.andthereitgoes.shadepile.transpiler.lua.transpile;

import org.jetbrains.annotations.Contract;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.PositionedNode;


public class LuaCompileError extends RuntimeException {
  public LuaCompileError(String message) {
    super(message);
  }

  public int line = 0;
  public int column = 0;

  @Contract("_,_ -> this")
  public LuaCompileError at(int line, int column) {
    this.line = line;
    this.column = column;
    return this;
  }
  @Contract("_ -> this")
  public LuaCompileError at(Token t) {
    this.line = t.line;
    this.column = t.column;
    return this;
  }
  @Contract("_ -> this")
  public LuaCompileError at(PositionedNode other) {
    this.line = other.line;
    this.column = other.column;
    return this;
  }
  @Contract("_,_ -> this")
  public LuaCompileError orAt(int line, int column) {
    if (this.line == 0 && this.column == 0) this.at(line, column);
    return this;
  }
  @Contract("_ -> this")
  public LuaCompileError orAt(Token t) {
    if (this.line == 0 && this.column == 0) this.at(t);
    return this;
  }
  @Contract("_ -> this")
  public LuaCompileError orAt(PositionedNode other) {
    if (this.line == 0 && this.column == 0) this.at(other);
    return this;
  }
}
