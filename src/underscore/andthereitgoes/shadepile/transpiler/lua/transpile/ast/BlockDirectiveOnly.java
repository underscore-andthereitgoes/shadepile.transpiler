package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


@FunctionalInterface
public interface BlockDirectiveOnly extends BlockDirective {
  @Override
  default void emit(NewlineCountingStringBuilder builder) {}

  @Override
  default void emitNewlines(NewlineCountingStringBuilder builder) {}

  @Override
  default int getLine() { return 0; }
}
