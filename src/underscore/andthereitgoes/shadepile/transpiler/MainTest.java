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
        for i = 1, 10 do
          print(i)
        end
        """;

    Tokenizer tokenizer = new Tokenizer(inputCode);
    Token[] tokens = tokenizer.flush();
    Parser parser = new Parser(tokens);
    Block out = parser.parse();
    NewlineCountingStringBuilder code = new NewlineCountingStringBuilder();
    out.emit(code);
    String outCode = code.toString();
    System.out.println(outCode);
  }

}
