package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.LuaCompileError;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.*;


public class Block extends PositionedNode {

  private static long blockId = 0;
  private static String newBlockId() {
    long id = blockId++;
    return Long.toUnsignedString(id, 36);
  }

  private final List<String> locals;
  private final Map<String,Integer> predefinedLocals;
  private final Map<String,String> predefinedLocalValueCode;
  private final Set<String> explicitGlobals;
  private boolean explicitGlobalsAll;
  private final Map<String,Label> labels;
  private final String groupName;
  private final List<StatementLike> statements;
  private boolean returned = false;
  private boolean anyGoto = false;
  private @Nullable Block parent;
  private boolean inheritLabels;
  private boolean canBreak;
  private final Set<Block> labelChildren;

  public Block(@Nullable Block parent) {
    this(parent, true);
  }

  public Block(@Nullable Block parent, boolean inheritLabels) {
    super();
    this.locals = new ArrayList<>();
    this.predefinedLocals = new HashMap<>();
    this.predefinedLocalValueCode = new HashMap<>();
    this.explicitGlobals = new HashSet<>();
    this.explicitGlobalsAll = false;
    this.labels = new HashMap<>();
    this.groupName = Block.newBlockId();
    this.statements = new ArrayList<>();
    this.returned = false;
    this.parent = parent;
    this.inheritLabels = inheritLabels;
    this.canBreak = parent != null && parent.canBreak;
    this.labelChildren = new HashSet<>();
  }

  @Contract("-> this")
  public Block enableBreak() {
    this.canBreak = true;
    return this;
  }

  @Contract(pure = true)
  public String getGroupName() {
    return groupName;
  }

  private void propagateGoto() {
    this.anyGoto = true;
    if (this.inheritLabels && this.parent != null) this.parent.propagateGoto();
  }

  public void pushStatement(StatementLike statementLike) {
    if (statementLike instanceof BlockDirective directive) {
      directive.onPush(this);
      if (statementLike instanceof BlockDirectiveOnly) return;
    }
    if (!(statementLike instanceof PositionedNode positionedNode)) throw new IllegalStateException();
    if (this.returned) throw new LuaCompileError("can't do more statements after returning in a block" + ((statementLike instanceof Empty) ? " (even empty statements, except one semicolon after return)" : "")).at(positionedNode);
    this.statements.add(statementLike);
    if (statementLike instanceof HasChildBlock hcb) {
      hcb.getChildBlocks().forEach(childBlock -> {
        if (childBlock.inheritLabels) this.labelChildren.add(childBlock);
        if (childBlock.anyGoto && !this.anyGoto) this.propagateGoto();
      });
    }
    switch (positionedNode) {
      case Assignment assignment -> {
        VariableOrPropertyAccess[] variables = assignment.variables;
        for (int i = 0, variablesLength = variables.length; i < variablesLength; i++) {
          VariableOrPropertyAccess vOrPA = variables[i];
          if (vOrPA instanceof VariableReference varRef) {
            if (varRef.block == this && varRef.localName != null) {
              variables[i] = this.findVariableOrDeclareLocal(varRef.localName);
            }
          }
        }
      }
      case Return ret -> this.returned = true;
      case Break brk -> {
        if (!this.canBreak) throw new LuaCompileError("can only break out of a loop, and can't call break after another break in the same block");
        this.canBreak = false;
      }
      case Label label -> {
        if (this.containsLabel(label.name)) throw new LuaCompileError("label ::" + label.name + ":: has already been defined in this scope");
        this.labels.put(label.name, label);
      }
      case Goto goer when !this.anyGoto -> this.propagateGoto();
      default -> {}
    }
  }

  public @NotNull Label findLabel(String name) {
    Label label = Optional.ofNullable(this.labels.get(name)).orElseGet(() -> this.labelChildren.stream().map(block -> block.findLabel(name)).findAny().orElse(null));
    if (label == null) throw new LuaCompileError("label ::" + name + ":: is not declared in the current scope");
    return label;
  }

  public boolean containsLabel(String name) {
    return this.labels.containsKey(name) || this.labelChildren.stream().anyMatch(block -> block.containsLabel(name));
  }

  public VariableReference predefineLocal(String name, String valueExpression) {
    int i = this.locals.size();
    this.predefinedLocals.put(name, i);
    this.predefinedLocalValueCode.put(name, valueExpression);
    this.locals.add(name);
    return VariableReference.local(name, this, i);
  }

  /// Declares a local variable by its name, adding it to this block's scope. Does nothing if the variable is already declared. Returns a reference to that variable.
  public @NotNull VariableReference findVariableOrDeclareLocal(String name) {
    if (!this.locals.contains(name)) {
      if (this.anyGoto) throw new LuaCompileError("can't declare local variables after a potential goto statement");
      this.locals.add(name);
    }
    return VariableReference.local(name, this, this.locals.indexOf(name));
  }

  /// Finds a local variable in this scope or parent scopes, returning `null` if none was found.
  public @Nullable VariableReference findVariable(String name) {
    if (this.locals.contains(name)) return VariableReference.local(name, this, this.locals.indexOf(name));
    Block parent = this.parent;
    if (parent == null || this.explicitGlobalsAll || this.explicitGlobals.contains(name)) return null;
    return parent.findVariable(name);
  }

  /// Finds a local variable in this scope or parent scopes, or returns a reference to a global variable if none is found.
  public @NotNull VariableOrPropertyAccess findVariableOrGlobal(String name) {
    VariableReference variable = findVariable(name);
    if (variable != null) return variable;
    return new PropertyAccess(VariableReference.env(), new Literal.StringLiteral(name));
  }

  public @NotNull String getJavaLabelName() {
    return "$_$" + this.groupName;
  }
  public @NotNull String getLabelVariableName() {
    return "$l$" + this.groupName;
  }

  public void emitWithoutBrackets(NewlineCountingStringBuilder builder) {
    String scopeVarName = "scope$" + this.groupName;
    builder.append("Object[] ");
    builder.append(scopeVarName);
    builder.append("=new Object[");
    builder.append(String.valueOf(this.locals.size()));
    builder.append("];");
    for (Map.Entry<String,Integer> entry: this.predefinedLocals.entrySet()) {
      builder.append(scopeVarName);
      builder.append('[');
      builder.append(String.valueOf((int)entry.getValue()));
      builder.append("]=");
      builder.append(this.predefinedLocalValueCode.get(entry.getKey()));
      builder.append(';');
    }
    if (this.labels.isEmpty()) {
      emitStatements(builder);
    } else {
      String labelVariableName = this.getLabelVariableName();
      builder.append("String ");
      builder.append(labelVariableName);
      builder.append("=\"@\";do{");
      builder.append(this.getJavaLabelName());
      builder.append(":{String $=");
      builder.append(labelVariableName);
      builder.append(';');
      builder.append(labelVariableName);
      builder.append("=\"\";switch($){case \"@\":");
      emitStatements(builder);
      builder.append("}}}while(");
      builder.append(labelVariableName);
      builder.append("!=\"\");");
    }
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append('{');
    this.emitWithoutBrackets(builder);
    builder.append('}');
  }

  private void emitStatements(NewlineCountingStringBuilder builder) {
    for (StatementLike statement: this.statements) {
      statement.emitNewlines(builder);
      statement.emit(builder);
      if (statement instanceof FunctionCall) builder.append(';');
    }
  }
}
