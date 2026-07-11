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
        
        while true do
          if y == 10 then break end
          y = y + 1
        end
        
        repeat
          local z = y + 2
          y = z
        until z > 24
        
        for i = 1, 10 do
          for j = 1, 50, 2 do
            print(i + j + 3 + 5 // 2)
            print((i << j) & i | j) -- oh yeah also this
          end
        end
        
        local table = {["key"] = "value"}
        
        for k, v in pairs(table) do
          print(k, v)
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
