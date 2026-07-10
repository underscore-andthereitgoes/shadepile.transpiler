package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public interface CodeEmitter {
  String environmentParameterName = "environment";
  String runtimeParameterName = "runtime";

  void emit(NewlineCountingStringBuilder builder);

  /// Emits newlines into a builder until the builder's current line matches this node's line.
  /// Statements should not need to call this on themselves, but expressions may.
  default void emitNewlines(NewlineCountingStringBuilder builder) {
    int lineDiff = this.getLine() - builder.getLine();
    if (lineDiff > 0) builder.repeatNewline(lineDiff);
  }

  int getLine();

}
