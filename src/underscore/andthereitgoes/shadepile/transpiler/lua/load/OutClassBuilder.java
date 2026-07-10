package underscore.andthereitgoes.shadepile.transpiler.lua.load;

import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.CodeEmitter;

import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.ProviderNotFoundException;
import java.util.*;


public class OutClassBuilder {

  public final StringBuilder code;

  private JavaFileObject file;
  private String className;

  public OutClassBuilder() {
    code = new StringBuilder();
    String uuidS = UUID.randomUUID().toString();
    className = "CompiledLua_" + uuidS.replace("-", "");
  }

  public void imports() {
    code.append("import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.*;");
  }

  public void prefix() {
    code.append("public final class ");
    code.append(className);
    code.append("{public static Object[] run(LuaRuntime ");
    code.append(CodeEmitter.runtimeParameterName);
    code.append(",LuaEnvironment ");
    code.append(CodeEmitter.environmentParameterName);
    code.append("){");
  }

  public void suffix() {
    code.append("return new Object[0];}}");
  }

  public void ready() {
    final String code = this.code.toString();
    file = new TranspiledLuaSourceFile(code, className);
  }

  /// Compiles the code this builder was given. Make sure you call `ready()` first.
  public CompiledLuaClassFile compile() {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) throw new ProviderNotFoundException("System Java Compiler not found for some reason");
    StandardJavaFileManager fym = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);
    MemoryFileManager mfm = new MemoryFileManager(fym);
    JavaCompiler.CompilationTask task = compiler.getTask(null, mfm, null, null, null, Collections.singletonList(this.file));
    if (!task.call()) throw new RuntimeException("Compilation failed: Compiler call() returned false");
    return mfm.compiledClasses.values().stream().findAny().orElseThrow(() -> new RuntimeException("Compilation failed: No class file output"));
  }

}
