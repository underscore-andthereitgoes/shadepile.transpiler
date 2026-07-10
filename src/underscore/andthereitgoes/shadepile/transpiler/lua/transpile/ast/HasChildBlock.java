package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import java.util.Set;


public interface HasChildBlock extends StatementLike {
  Set<Block> getChildBlocks();
}
