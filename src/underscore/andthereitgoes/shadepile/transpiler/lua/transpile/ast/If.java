package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.*;


public class If extends Statement implements HasChildBlock {

  public static final class Section {
    private final @NotNull Block body;
    private final @NotNull Expression condition;

    public Section(@NotNull Block body, @NotNull Expression condition) {
      this.body = body;
      this.condition = condition;
    }

    public @NotNull Block body() {
      return body;
    }

    public @NotNull Expression condition() {
      return condition;
    }

  }

  public final ArrayList<Section> sections = new ArrayList<>();
  public @Nullable Block elseBlock;
  private final HashSet<Block> allBlocks = new HashSet<>();
  private final Block parent;

  public If(Block parent) {
    this.parent = parent;
  }
  public If(@NotNull List<@NotNull Section> sections, @Nullable Block elseBlock, @NotNull Block parent) {
    this(parent);
    sections.forEach(this::addSection);
    if (elseBlock != null) this.addElse(elseBlock);
  }

  public Block addSection(Section section) {
    sections.add(section);
    allBlocks.add(section.body);
    return section.body;
  }

  public Block addSection(Expression condition) {
    Block body = new Block(parent, true);
    Section section = new Section(body, condition);
    sections.add(section);
    allBlocks.add(body);
    return body;
  }

  public Block addElse(Block elseBlock) {
    allBlocks.add(elseBlock);
    this.elseBlock = elseBlock;
    return elseBlock;
  }

  public Block addElse() {
    Block body = new Block(parent, true);
    allBlocks.add(body);
    this.elseBlock = body;
    return body;
  }

  @Override
  public Set<Block> getChildBlocks() {
    return allBlocks;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    for (int i = 0, numSections = sections.size(); i < numSections; i++) {
      Section section = sections.get(i);
      if (i != 0) builder.append("else ");
      builder.append("if(");
      builder.append(CodeEmitter.runtimeParameterName);
      builder.append(".o.asbool(");
      section.condition.emit(builder);
      builder.append("))");
      section.body.emit(builder);
    }
    if (this.elseBlock != null) {
      builder.append("else if(true)");
      this.elseBlock.emit(builder);
    }
  }
}
