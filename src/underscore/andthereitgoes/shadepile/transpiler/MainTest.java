package underscore.andthereitgoes.shadepile.transpiler;

import underscore.andthereitgoes.shadepile.transpiler.lua.LuaExecutionHelper;
import underscore.andthereitgoes.shadepile.transpiler.lua.load.CompiledLuaLoader;
import underscore.andthereitgoes.shadepile.transpiler.lua.parse.Parser;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaEnvironment;
import underscore.andthereitgoes.shadepile.transpiler.lua.runtime.LuaRuntime;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Tokenizer;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.Block;

import java.util.Arrays;


public class MainTest {

  static void main(String[] args) {

    String inputCode = """
        for i = 10, 1, -1 do
          print(i)
        end
        
        print("hello, world!")
        """;

    Tokenizer tokenizer = new Tokenizer(inputCode);
    Token[] tokens = tokenizer.flush();
    Parser parser = new Parser(tokens);
    Block out = parser.parse();
    NewlineCountingStringBuilder code = new NewlineCountingStringBuilder();
    out.emit(code);
    String outCode = code.toString();
    System.out.println(outCode);

    var runtime = new LuaRuntime();
    var environment = new LuaEnvironment(runtime) {
      @Override
      public void print(Object[] args) {
        System.out.println(String.join(" ", Arrays.stream(args).map(Object::toString).toArray(String[]::new)));
      }
    };
    environment.addGlobals();
    environment.addBit32Module();
    environment.addMathModule();
    environment.addStringModule(true);

    LuaExecutionHelper.loadLua(environment, inputCode);

//    System.out.println(Arrays.toString(runtime.f.fcall(runtime.p.get(runtime.p.refBind("hello, world!", "rep")), 2L)));
//    System.out.println(runtime.p.getMetafield("", "__index"));
//    System.out.println(runtime.p.refBind("", "sub"));
//    System.out.println(runtime.p.get(runtime.p.refBind("hello, world!", "rep")));

  }

}
