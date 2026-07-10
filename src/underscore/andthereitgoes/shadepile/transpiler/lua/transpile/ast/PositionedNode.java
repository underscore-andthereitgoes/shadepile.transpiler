package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.jetbrains.annotations.Contract;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;


@SuppressWarnings("UnusedReturnValue")
public abstract class PositionedNode implements CodeEmitter {

  @Override
  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public int line = 0;
  public int column = 0;

  @Contract("_,_ -> this")
  public PositionedNode at(int line, int column) {
    this.line = line;
    this.column = column;
    return this;
  }
  @Contract("_ -> this")
  public PositionedNode at(Token t) {
    this.line = t.line;
    this.column = t.column;
    return this;
  }
  @Contract("_ -> this")
  public PositionedNode at(PositionedNode other) {
    this.line = other.line;
    this.column = other.column;
    return this;
  }
  @Contract("_,_ -> this")
  public PositionedNode orAt(int line, int column) {
    if (this.line == 0 && this.column == 0) this.at(line, column);
    return this;
  }
  @Contract("_ -> this")
  public PositionedNode orAt(Token t) {
    if (this.line == 0 && this.column == 0) this.at(t);
    return this;
  }
  @Contract("_ -> this")
  public PositionedNode orAt(PositionedNode other) {
    if (this.line == 0 && this.column == 0) this.at(other);
    return this;
  }
}
