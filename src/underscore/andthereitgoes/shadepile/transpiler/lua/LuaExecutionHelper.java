package underscore.andthereitgoes.shadepile.transpiler.lua;

import underscore.andthereitgoes.shadepile.transpiler.lua.load.CompiledLuaClassFile;
import underscore.andthereitgoes.shadepile.transpiler.lua.load.CompiledLuaLoader;
import underscore.andthereitgoes.shadepile.transpiler.lua.load.OutClassBuilder;
import underscore.andthereitgoes.shadepile.transpiler.lua.parse.LuaParseError;
import underscore.andthereitgoes.shadepile.transpiler.lua.parse.Parser;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaEnvironment;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaRuntime;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.LuaSyntaxError;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Tokenizer;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.LuaCompileError;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.Block;


public final class LuaExecutionHelper {

  public static Object loadJava(LuaEnvironment environment, String javaCode) throws RuntimeException {
    OutClassBuilder ocb = new OutClassBuilder();
    ocb.imports();
    ocb.prefix();
    ocb.code.append(javaCode);
    ocb.suffix();
    ocb.ready();
    CompiledLuaClassFile classFile = ocb.compile();
    CompiledLuaLoader loader = new CompiledLuaLoader(classFile.toByteArray());
    return loader.run(new LuaRuntime(), environment);
  }

  @SuppressWarnings("DuplicateThrows")
  public static Object loadLua(LuaEnvironment environment, String luaCode) throws LuaSyntaxError, LuaParseError, LuaCompileError, RuntimeException {

    Tokenizer tokenizer = new Tokenizer(luaCode);
    Token[] tokens = tokenizer.flush();
    Parser parser = new Parser(tokens);
    Block out = parser.parse();
    NewlineCountingStringBuilder codeBuilder = new NewlineCountingStringBuilder();
    out.emit(codeBuilder);
    String outCode = codeBuilder.toString();

    OutClassBuilder ocb = new OutClassBuilder();
    ocb.imports();
    ocb.prefix();
    ocb.code.append(outCode);
    ocb.suffix();
    ocb.ready();
    CompiledLuaClassFile classFile = ocb.compile();
    CompiledLuaLoader loader = new CompiledLuaLoader(classFile.toByteArray());
    return loader.run(new LuaRuntime(), environment);
  }

}
