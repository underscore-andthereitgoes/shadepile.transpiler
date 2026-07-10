package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;


public class Goto extends Statement implements BlockDirective {
  public final String targetLabelName;
  public Block block;

  public Goto(String targetLabelName) {
    this.targetLabelName = targetLabelName;
  }

  @Override
  public void onPush(Block block) {
    this.block = block;
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    if (this.block == null) throw new IllegalStateException("goto with unassigned block asked to emit");
    Label targetLabel = this.block.findLabel(this.targetLabelName);
    builder.append(targetLabel.block.getLabelVariableName());
    builder.append("=\"");
    builder.append(targetLabel.name);
    builder.append("\";if(true)break ");
    builder.append(targetLabel.block.getJavaLabelName());
    builder.append(';');
  }
}
