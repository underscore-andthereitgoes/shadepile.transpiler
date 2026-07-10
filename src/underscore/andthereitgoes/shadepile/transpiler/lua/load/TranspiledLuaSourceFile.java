package underscore.andthereitgoes.shadepile.transpiler.lua.load;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;


public class TranspiledLuaSourceFile extends SimpleJavaFileObject {

  private final CharSequence code;

  public TranspiledLuaSourceFile(CharSequence code, String className) {
    URI uri = URI.create("string:///lua/transpiled/" + className + Kind.SOURCE.extension);
    super(uri, Kind.SOURCE);
    this.code = code;
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
    return code;
  }

}
