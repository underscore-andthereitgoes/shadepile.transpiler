package underscore.andthereitgoes.shadepile.transpiler.lua.load;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
  public final Map<String, CompiledLuaClassFile> compiledClasses = new HashMap<>();

  public MemoryFileManager(StandardJavaFileManager fileManager) {
    super(fileManager);
  }

  @Override
  public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
    if (kind == Kind.CLASS) {
      CompiledLuaClassFile compiledClassFile = new CompiledLuaClassFile(className, kind);
      compiledClasses.put(className, compiledClassFile);
      return compiledClassFile;
    }
    return super.getJavaFileForOutput(location, className, kind, sibling);
  }

  public byte[] getBytecode(String className) {
    return compiledClasses.get(className).toByteArray();
  }
}
