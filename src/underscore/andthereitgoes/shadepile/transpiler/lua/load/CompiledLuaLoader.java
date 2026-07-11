package underscore.andthereitgoes.shadepile.transpiler.lua.load;

import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaEnvironment;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaRuntime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class CompiledLuaLoader extends ClassLoader {
  private final byte[] bytecode;
  private Class<?> defined = null;
  private boolean hasRun = false;

  public CompiledLuaLoader(byte[] bytecode) {
    this.bytecode = bytecode;
  }

  public Class<?> define() {
    if (this.defined != null) throw new IllegalStateException("Can't load bytecode or define() more than once on the same CompiledLuaLoader instance.");
    return (this.defined = defineClass(null, bytecode, 0, bytecode.length));
  }

  public Object run(LuaRuntime runtime, LuaEnvironment environment) {
    if (this.hasRun) throw new IllegalStateException("Can't call run() more than once on the same CompiledLuaLoader instance.");
    if (this.defined == null) this.defined = this.define();
    this.hasRun = true;
    final Method method;
    try {
      method = this.defined.getDeclaredMethod("run", LuaRuntime.class, LuaEnvironment.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Failed to run CompiledLua class (may not have compiled correctly).", e);
    }
    try {
      return method.invoke(null, runtime, environment);
    } catch (RuntimeException | InvocationTargetException e) {
      throw new RuntimeException("Error in Lua code.", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Error while invoking Lua code (unrelated to Lua).", e);
    }
  }
}
