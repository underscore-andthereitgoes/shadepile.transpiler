package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


/// Implement to do stuff to a statement when it's pushed into a block. Also see `BlockDirectiveOnly`.
public interface BlockDirective extends StatementLike {
  void onPush(Block block);
}
