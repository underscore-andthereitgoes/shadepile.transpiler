package underscore.andthereitgoes.shadepile.transpiler.lua;

import underscore.andthereitgoes.shadepile.transpiler.lua.load.CompiledLuaClassFile;
import underscore.andthereitgoes.shadepile.transpiler.lua.load.CompiledLuaLoader;
import underscore.andthereitgoes.shadepile.transpiler.lua.load.OutClassBuilder;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaEnvironment;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaRuntime;

import static underscore.andthereitgoes.shadepile.transpiler.lua.__PLACEHOLDER__.doSomething;


public final class LuaExecutionHelper {

  public Object loadJava(LuaEnvironment environment, String javaCode) throws RuntimeException {
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

  public Object loadLua(LuaEnvironment environment, String luaCode) throws RuntimeException {
    OutClassBuilder ocb = new OutClassBuilder();
    ocb.imports();
    ocb.prefix();

    // TODO: insert transpiler stuff here
    doSomething(luaCode, ocb.code);

    ocb.suffix();
    ocb.ready();
    CompiledLuaClassFile classFile = ocb.compile();
    CompiledLuaLoader loader = new CompiledLuaLoader(classFile.toByteArray());
    return loader.run(new LuaRuntime(), environment);
  }

}
