package underscore.andthereitgoes.shadepile.transpiler.lua.load;

import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.net.URI;


public class CompiledLuaClassFile extends SimpleJavaFileObject {

  private final ByteArrayOutputStream out;
  public final String className;

  protected CompiledLuaClassFile(String className, Kind kind) {
    super(URI.create("string:///lua/compiled/" + className.replace(".", "/") + kind.extension), kind);
    this.className = className;
    this.out = new ByteArrayOutputStream();
  }

  @Override
  public OutputStream openOutputStream() throws IOException {
    return out;
  }

  public byte[] toByteArray() {
    return out.toByteArray();
  }
}
