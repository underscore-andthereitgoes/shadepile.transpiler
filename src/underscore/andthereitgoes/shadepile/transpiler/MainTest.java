package underscore.andthereitgoes.shadepile.transpiler;

import com.sun.jdi.request.BreakpointRequest;
import underscore.andthereitgoes.shadepile.transpiler.lua.parse.Parser;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Tokenizer;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.Block;


public class MainTest {

  static void main(String[] args) {
    String inputCode = """
        
        print("hello, world!")
        print("another statement please")
        
        if true then print("test") end
        
        if "test" ~= "not" then
          do
            local x = 5
          end
        end
        
        local y = 3
        while y < 10 do
          y = y + 1
        end
        
        """;

    Tokenizer tokenizer = new Tokenizer(inputCode);
    Token[] tokens = tokenizer.flush();
//    breakpoint(tokens);
    Parser parser = new Parser(tokens);
    Block out = parser.parse();
    breakpoint(out);
    NewlineCountingStringBuilder code = new NewlineCountingStringBuilder();
    out.emit(code);
    String outCode = code.toString();
    breakpoint(outCode);
    System.out.println(outCode);
  }

  public static void breakpoint(Object... args) {}

}
