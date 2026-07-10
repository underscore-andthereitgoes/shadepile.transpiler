package underscore.andthereitgoes.shadepile.transpiler.lua.tokenize;

import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.KeywordTokenType;
import underscore.andthereitgoes.shadepile.transpiler.lua.OperatorTokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


public class Tokenizer extends StringReader {

  private static void ok() {}

  private static final Set<Character> IDENTIFIER1_c = Set.of('A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','_');
  private static final Set<Character> IDENTIFIER_c = Set.of('A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','_','0','1','2','3','4','5','6','7','8','9');
  private static final Set<String> IDENTIFIER1 = Set.of("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","_");
  private static final Set<String> IDENTIFIER = Set.of("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","_","0","1","2","3","4","5","6","7","8","9");
  private static final Set<Character> DEC_c = Set.of('0','1','2','3','4','5','6','7','8','9');
  private static final Set<Character> HEX1_c = Set.of('0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f','A','B','C','D','E','F');
  private static final Set<String> HEX1 = Set.of("0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f","A","B","C","D","E","F");
  private static final Set<String> HEX2;
  static {
    String[] hex2 = new String[HEX1.size()*HEX1.size()];
    AtomicInteger idx = new AtomicInteger();
    HEX1.forEach(ch1 -> {
      HEX1.forEach(ch2 -> {
        hex2[idx.getAndIncrement()] = ch1 + ch2;
      });
    });
    HEX2 = Set.of(hex2);
  }

  private final ArrayList<Token.AnyBracket> openedBrackets = new ArrayList<>();

  public enum BracketType {
    PAREN('(',')'),
    SQUARE('[',']'),
    CURLY('{','}'),
    ;

    public final char left;
    public final char right;

    BracketType(char left, char right) {
      this.left = left;
      this.right = right;
    }

    public static BracketType byLeft(char left) {
      return Arrays.stream(values()).filter(bracketType -> bracketType.left == left).findFirst().orElse(null);
    }
    public static BracketType byRight(char right) {
      return Arrays.stream(values()).filter(bracketType -> bracketType.right == right).findFirst().orElse(null);
    }
  }

  public Tokenizer(String input) {
    super(input);
  }

  private void skip() {
    do this.skipSpace();
    while (this.takeComment());
  }

  private void skipSpace() {
    while (this.hasMore() && Character.isWhitespace(this.peek1())) this.take1();
  }

  private void skipLine() {
    while (this.hasMore() && this.take1() != '\n') ok();
  }

  private boolean takeComment() {
    if (this.peek(2).equals("--")) {
      this.take(2);
      if (this.takeLongBracket() == null) this.skipLine();
      return true;
    }
    return false;
  }

  private boolean checkForOpeningLongBracket() {
    if (this.hasMore() && this.peek1() == '[') {
      int state = 0;
      for (char ch: this.peek(6).toCharArray()) {
        if (ch == '[') {
          state++;
          if (state == 2) return true;
        } else if (ch == '=') {
          if (state != 1) break;
        } else break;
      }
    }
    return false;
  }

  private @Nullable String takeLongBracket() {
    if (this.checkForOpeningLongBracket()) {
      StringBuilder txt = new StringBuilder();
      this.take1();
      StringBuilder closeB = new StringBuilder("]");
      while (this.peek1() == '=') closeB.append(this.take1());
      closeB.append(']');
      String close = closeB.toString();
      int cl = close.length();
      while (!this.peek(cl).equals(close)) {
        txt.append(this.take1());
        if (!this.hasMore()) throw new LuaSyntaxError("unexpected end of file inside long bracket");
      }
      this.take(cl);
      return txt.toString();
    }
    return null;
  }

  private @Nullable Token.StringLiteral takeStringLiteral() {
    if (!this.hasMore()) return null;
    final char q = this.peek1();
    if (q == '\'' || q == '"') {
      this.take1();
      StringBuilder str = new StringBuilder();

      char ch;
      while (true) {
        if (!this.hasMore()) throw new LuaSyntaxError("unexpected end of input inside string");
        ch = this.take1();

        if (ch == q) break;
        else {
          if (ch == '\\') {
            if (!this.hasMore()) throw new LuaSyntaxError("unexpected end of input after \\ in string");

            char characterAfterBackslash = this.take1();

            switch (characterAfterBackslash) {

              case '\\', '\'', '"' -> ch = characterAfterBackslash;

              case 'a' -> ch = '\07';
              case 'b' -> ch = '\b';
              case 'f' -> ch = '\f';
              case 'n' -> ch = '\n';
              case 'r' -> ch = '\r';
              case 't' -> ch = '\t';
              case 'v' -> ch = '\13';
              case 'z' -> {
                while (this.hasMore() && Character.isWhitespace(this.peek1())) this.take1();
                continue; // skip adding `ch`
              }

              case 'x' -> {
                String hex = this.take(2);
                if (!HEX2.contains(hex))
                  throw new LuaSyntaxError("expected two hexadecimal digits after \\x in string");
                ch = (char)Integer.parseUnsignedInt(hex.toLowerCase(), 16);
              }

              case 'u' -> {
                if (!this.hasMore() || this.take1() != '{')
                  throw new LuaSyntaxError("expected { after \\u in string");

                StringBuilder hex = new StringBuilder();
                while (HEX1_c.contains(this.peek1())) hex.append(this.take1());

                if (hex.isEmpty())
                  throw new LuaSyntaxError("expected one or more hexadecimal digits after \\u{ in string");

                if (!this.hasMore() || this.take1() != '}')
                  throw new LuaSyntaxError("expected } after \\u{... in string");

                String hexs = hex.toString().toLowerCase();

                try {
                  int code = Integer.parseUnsignedInt(hexs, 16);
                  str.appendCodePoint(code);
                  continue; // we added the code point already
                } catch (NumberFormatException e) {
                  throw new LuaSyntaxError("hexadecimal escape codepoint too large: " + hexs);
                }
              }

              case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                StringBuilder dec = new StringBuilder(String.valueOf(characterAfterBackslash));
                while (this.hasMore() && DEC_c.contains(this.peek1()) && dec.length() < 3) dec.append(this.take1());
                ch = (char)Integer.parseUnsignedInt(dec.toString(), 10);
              }

              default -> throw new LuaSyntaxError("invalid escape in string: \\" + ch);

            }
            // fall through with whatever `ch` is (unless `continue` happened)
          }
          str.append(ch);
        }
      }
      return new Token.StringLiteral(str.toString());
    }
    return null;
  }

  private static @Nullable Integer takeExponent(StringReader r) {
    if (!r.hasMore()) return null;
    StringBuilder nstr = new StringBuilder();
    char s = r.peek1();
    if (s == '-' || s == '+') nstr.append(s);
    else nstr.append('+');
    while (r.hasMore() && DEC_c.contains(r.peek1())) nstr.append(r.take1());
    if (nstr.length() > 1) return null;
    return Integer.parseInt(nstr.toString(), 10);
  }

  private @Nullable Token.NumberLiteral takeNumberLiteral() {
    if (!this.hasMore()) return null;
    switch (this.peek1()) {
      case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
        try {
          char ch0 = this.take1();

          StringBuilder numStrB = new StringBuilder(String.valueOf(ch0));

          char p = this.peek1();
          if (ch0 == '0' && (p == 'X' || p == 'x')) {

            do numStrB.append(this.take1()); // "0x" then hex
            while (this.hasMore() && HEX1_c.contains(this.peek1()));

            String numStr = numStrB.toString().toLowerCase();

            if (!numStr.equals("0x")) {
              Number num = Long.parseUnsignedLong(numStr.substring(2), 16);
              boolean floating = false;

              if (this.peek1() == '.') {
                this.take1();
                StringBuilder fracB = new StringBuilder();
                while (this.hasMore() && HEX1_c.contains(this.peek1())) fracB.append(this.take1());
                if (!fracB.isEmpty()) {
                  num = num.doubleValue() + (double)Long.parseUnsignedLong(fracB.toString(), 16) / Math.pow(16d, fracB.length());
                  floating = true;
                } else {
                  throw new LuaSyntaxError("expected fractional digits after hexadecimal point in number literal");
                }
              }

              if (Character.toLowerCase(this.peek1()) == 'p') {
                this.take1();
                Integer exponent = takeExponent(this);
                if (exponent != null) {
                  num = num.doubleValue() * Math.pow(2d, exponent);
                  floating = true;
                } else {
                  throw new LuaSyntaxError("expected exponent after P in hexadecimal literal");
                }
              }

              return new Token.NumberLiteral(floating ? num.doubleValue() : num.longValue());

            } else throw new LuaSyntaxError("expected hexadecimal digits after 0x");

          } else {

            while (DEC_c.contains(this.peek1())) numStrB.append(this.take1());
            String numStr = numStrB.toString();

            Number num = Long.parseUnsignedLong(numStr, 10);
            boolean floating = false;

            if (this.peek1() == '.') {
              this.take1();
              StringBuilder fracB = new StringBuilder();
              while (this.hasMore() && DEC_c.contains(this.peek1())) fracB.append(this.take1());
              if (!fracB.isEmpty()) {
                num = num.doubleValue() + (double)Long.parseUnsignedLong(fracB.toString(), 10) / Math.pow(10d, fracB.length());
                floating = true;
              } else {
                throw new LuaSyntaxError("expected fractional digits after decimal point in number literal");
              }
            }

            if (Character.toLowerCase(this.peek1()) == 'e') {
              this.take1();
              Integer exponent = takeExponent(this);
              if (exponent != null) {
                num = num.doubleValue() * Math.pow(10d, exponent);
                floating = true;
              } else {
                throw new LuaSyntaxError("expected exponent after E in decimal literal");
              }
            }

            return new Token.NumberLiteral(floating ? num.doubleValue() : num.longValue());
          }
        } catch (NumberFormatException e) {
          throw new LuaSyntaxError("unable to parse number");
        }
      }
    }
    return null;
  }

  private @Nullable Token.AnyBracket takeBracket() {
    if (!this.hasMore()) return null;
    char ch = this.peek1();
    Token.AnyBracket currentlyOpenBracket = this.openedBrackets.isEmpty() ? null : this.openedBrackets.getLast();
    for (BracketType bracket: BracketType.values()) {
      if (ch == bracket.left) {
        this.take1();
        Token.AnyBracket token = switch (bracket) {
          case PAREN -> new Token.Parenthesis(Token.BracketSide.LEFT);
          case SQUARE -> new Token.SquareBracket(Token.BracketSide.LEFT);
          case CURLY -> new Token.CurlyBracket(Token.BracketSide.LEFT);
        };
        this.openedBrackets.addLast(token);
        return token;
      } else if (ch == bracket.right) {
        this.take1();
        if (currentlyOpenBracket != null) {
          if (currentlyOpenBracket.type == bracket) {
            this.openedBrackets.removeLast();
            return switch (bracket) {
              case PAREN -> new Token.Parenthesis(Token.BracketSide.RIGHT);
              case SQUARE -> new Token.SquareBracket(Token.BracketSide.RIGHT);
              case CURLY -> new Token.CurlyBracket(Token.BracketSide.RIGHT);
            };
          } else {
            throw new LuaSyntaxError("mismatched brackets: opened with " + currentlyOpenBracket.type.left + " but attempted to close with " + ch);
          }
        } else {
          throw new LuaSyntaxError(ch + " doesn't close anything");
        }
      }
    }
    return null;
  }

  private @Nullable Token.AnySeparator takeSeparator() {
    if (!this.hasMore()) return null;
    return switch (this.peek1()) {
      case ';' -> { this.take1(); yield new Token.Semicolon(); }
      case ',' -> { this.take1(); yield new Token.Comma(); }
      default -> null;
    };
  }

  private @Nullable Token.ThreeDots takeThreeDots() {
    if (this.peek(3).equals("...")) {
      this.take(3);
      return new Token.ThreeDots();
    }
    return null;
  }

  private @Nullable Token.Operator takeOperator() {
    for (var op: OperatorTokenType.descendingLength) {
      String t = op.text;
      int len = t.length();
      if (this.peek(len).equals(t)) {
        if (IDENTIFIER_c.contains(t.charAt(len - 1))) {
          if (IDENTIFIER.contains(this.look(len))) continue;
        }
        this.take(len);
        return new Token.Operator(op);
      }
    }
    return null;
  }

  private @Nullable Token.PropertyAccess takePropertyAccess() {
    if (!this.hasMore()) return null;
    return switch (this.peek1()) {
      case ':' -> { this.take1(); yield new Token.PropertyColon(); }
      case '.' -> { this.take1(); yield new Token.PropertyDot(); }
      default -> null;
    };
  }

  private @Nullable Token.SingleEquals takeSingleEquals() {
    if (!this.hasMore()) return null;
    if (this.peek1() == '=') {
      this.take1();
      return new Token.SingleEquals();
    }
    return null;
  }

  private @Nullable Token.Name takeName() {
    if (!this.hasMore()) return null;
    if (IDENTIFIER1_c.contains(this.peek1())) {
      StringBuilder name = new StringBuilder();
      while (IDENTIFIER_c.contains(this.peek1())) name.append(this.take1());
      return new Token.Name(name.toString());
    }
    return null;
  }

  private @Nullable Token.Keyword takeKeyword() {
    for (var kw: KeywordTokenType.descendingLength) {
      String t = kw.text;
      int len = t.length();
      if (this.peek(len).equals(t)) {
        if (IDENTIFIER_c.contains(t.charAt(len - 1))) {
          if (IDENTIFIER.contains(this.look(len))) continue;
        }
        this.take(len);
        return new Token.Keyword(kw);
      }
    }
    return null;
  }

  private @Nullable Token.LabelBoundary takeLabelBoundary() {
    if (this.peek(2).equals("::")) {
      this.take(2);
      return new Token.LabelBoundary();
    }
    return null;
  }

  public @Nullable Token next() {
    this.skip();

    int line = this.getLine();
    int column = this.getColumn();

    // string > bracket;
    // number > operator;
    // three dots > operator;
    // operator > property access;
    // three dots > property access;
    // operator > single equals;
    // string > name;
    // number > name;
    // keyword > operator;
    // keyword > name;
    // operator > name;
    // keyword > operator > name;
    // label boundary > property access;
    // separator doesn't conflict with anything;

    // (((number, three_dots, keyword) > operator > ((string > (bracket, name)), (label_boundary, property_access), single_equals)), separator)

    try {
      Token token;
      token = this.takeNumberLiteral();
      if (token == null) token = this.takeThreeDots();
      if (token == null) token = this.takeKeyword();
      if (token == null) token = this.takeOperator();
      if (token == null) token = this.takeStringLiteral();
      if (token == null) token = this.takeBracket();
      if (token == null) token = this.takeName();
      if (token == null) token = this.takeLabelBoundary();
      if (token == null) token = this.takePropertyAccess();
      if (token == null) token = this.takeSingleEquals();
      if (token == null) token = this.takeSeparator();
      if (token != null) {
        token.line = line;
        token.column = column;
      }
      return token;
    } catch (LuaSyntaxError e) {
      e.line = line;
      e.column = column;
      throw e;
    }
  }

  public void end() {
    this.skip();

    if (this.hasMore()) {
      LuaSyntaxError e = new LuaSyntaxError("unexpected " + this.take1());
      e.line = this.getLine();
      e.column = this.getColumn();
      throw e;
    }
    if (!this.openedBrackets.isEmpty()) {
      var unclosedToken = this.openedBrackets.getLast();
      LuaSyntaxError e = new LuaSyntaxError("unclosed bracket: " + unclosedToken.type.left);
      e.line = unclosedToken.line;
      e.column = unclosedToken.column;
      throw e;
    }
  }

}
