package underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.LuaCompileError;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class FunctionDefinition extends Expression {

  public static final String javaArgumentsName = "$args";

  public final @NotNull String @NotNull [] parameters;
  public final @Nullable String restParameter;
  public final @NotNull VariableReference @NotNull [] parameterRefs;
  public final @Nullable VariableReference restParameterRef;
  public final @Nullable VariableReference restTableParameterRef;
  public final Block body;

  public FunctionDefinition(@NotNull String @NotNull [] parameters, boolean selfParameter, Block parent) {
    if (!Arrays.stream(parameters).allMatch(s -> s.matches("^(\\.\\.\\.)?([a-zA-Z_][a-zA-Z0-9_]*)?$"))) throw new LuaCompileError("invalid or empty parameter name found");

    this.restParameter = parameters.length > 0 && parameters[parameters.length - 1].startsWith("...") ? parameters[parameters.length - 1].substring(3) : null;
    String[] parametersExceptSelf = restParameter != null ? Arrays.copyOf(parameters, parameters.length - 1) : parameters;
    if (selfParameter) {
      ArrayList<String> parametersList = new ArrayList<>(List.of(parametersExceptSelf));
      parametersList.addFirst("self");
      this.parameters = parametersList.toArray(String[]::new);
    } else {
      this.parameters = parametersExceptSelf;
    }
    if (Arrays.stream(this.parameters).anyMatch(s -> s.startsWith("..."))) throw new LuaCompileError("rest parameter (...) may only be the last parameter");

    Set<String> parameterSet = new HashSet<>();
    if (this.restParameter != null) {
      parameterSet.add("...");
      if (!this.restParameter.isEmpty()) {
        parameterSet.add(this.restParameter);
      }
    }
    for (String parameter: this.parameters) {
      if (parameterSet.contains(parameter)) throw new LuaCompileError("duplicate parameter \""+parameter+"\"");
      parameterSet.add(parameter);
    }

    AtomicInteger i = new AtomicInteger();
    this.body = new Block(parent, false);
    this.parameterRefs = Arrays.stream(this.parameters)
        .map(parameter -> this.body.predefineLocal(
            parameter,
            javaArgumentsName + ".length>" + i + "?" + javaArgumentsName + "[" + i.getAndIncrement() + "]:null"
        ))
        .toArray(VariableReference[]::new)
    ;

    if (this.restParameter != null) {
      this.restParameterRef = this.body.predefineLocal(
          "...",
          CodeEmitter.runtimeParameterName + ".u.extractRest(" + javaArgumentsName + "," + this.parameters.length + ")"
      );
      if (!this.restParameter.isEmpty()) {
        this.restTableParameterRef = this.body.predefineLocal(
            this.restParameter,
            CodeEmitter.runtimeParameterName + ".u.extractRestTable(" + javaArgumentsName + "," + this.parameters.length + ")"
        );
      } else this.restTableParameterRef = null;
    } else {
      this.restParameterRef = null;
      this.restTableParameterRef = null;
    }
  }

  @Override
  public void emit(NewlineCountingStringBuilder builder) {
    builder.append(CodeEmitter.runtimeParameterName);
    builder.append(".f.def(((");
    builder.append(javaArgumentsName);
    builder.append(")->{");
    this.body.emit(builder);
    builder.append("return new Object[0];");
    builder.append("}))");
  }
}
