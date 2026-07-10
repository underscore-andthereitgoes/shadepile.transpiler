package underscore.andthereitgoes.shadepile.transpiler.lua.parse;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.BinaryOperator;
import underscore.andthereitgoes.shadepile.transpiler.lua.KeywordTokenType;
import underscore.andthereitgoes.shadepile.transpiler.lua.UnaryOperator;
import underscore.andthereitgoes.shadepile.transpiler.lua.__PLACEHOLDER__;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public class Parser {

  private static abstract class CallInfoOrPropertyPath {}

  private static class CallInfo extends CallInfoOrPropertyPath {
    public final @Nullable String method;
    public final @NotNull List<@NotNull Expression> arguments;

    private CallInfo(@Nullable String method, @NotNull List<@NotNull Expression> arguments) {
      this.method = method;
      this.arguments = arguments;
    }
  }

  private static class PropertyPath extends CallInfoOrPropertyPath {
    public final Expression item;

    private PropertyPath(Expression item) {
      this.item = item;
    }
  }

  private final Token[] input;
  private int pointer;

  public Parser(Token[] input) {
    this.input = input;
    this.pointer = 0;
  }

  @Contract(pure = true)
  public boolean available() {
    return pointer < input.length;
  }

  @Contract(pure = true)
  public int tell() {
    return Math.clamp(pointer, 0, input.length - 1);
  }
  public void seek(int to) {
    pointer = Math.clamp(to, 0, input.length - 1);
  }

  @Contract(pure = true)
  public @Nullable Token look(int offset) {
    int at = pointer + offset;
    return at >= 0 && at < input.length ? input[at] : null;
  }

  @Contract(pure = true)
  public Token peek() {
    return pointer < input.length ? input[pointer] : null;
  }

  public Token take() {
    return input[pointer = Math.min(pointer + 1, input.length - 1)];
  }

  @SuppressWarnings("unchecked")
  public Block parse() {
    final TokenFinder<Statement> block;
    final TokenFinder<?> s_end;
    final TokenFinder<If> s_if;
    final TokenFinder<Do> s_do;
    final TokenFinder<RepeatWhile> s_while;
    final TokenFinder<RepeatUntil> s_repeat;
    final TokenFinder<For> s_for;
    final TokenFinder<ForNumeric> s_fornum;
    final TokenFinder<ForGeneric> s_forgen;
    final TokenFinder<Goto> s_goto;
    final TokenFinder<Break> s_break;
    final TokenFinder<Return> s_return;
    final TokenFinder<Statement> stmt;
    final TokenFinder<Label> label;
    final TokenFinder<UnaryOperator> unop;
    final TokenFinder<BinaryOperator> binop;
    final TokenFinder<UnaryOperatorOrExpression> unopexpr;
    final TokenFinder<Expression> opexpr;
    final TokenFinder<Expression> term;
    final TokenFinder<Expression> groupexpr;
    final TokenFinder<Expression> expr;
    final TokenFinder<String> namelist;
    final TokenFinder<?> attr;
    final TokenFinder<String> attrnamelist;
    final TokenFinder<CallInfo> callpath;
    final TokenFinder<VariableOrPropertyAccess> props;
    final TokenFinder<Expression> propsfunccall;
    final TokenFinder<FunctionCall> funccall;
    final TokenFinder<@Nullable String> methodsuff;
    final TokenFinder<CallInfo> funccallsuff;
    final TokenFinder<PropertyPath> propertypathsuff;
    final TokenFinder<VariableOrPropertyAccess> simplevariable;
    final TokenFinder<VariableOrPropertyAccess> varlist;
    final TokenFinder<Assignment> assignment;
    final TokenFinder<Expression> assignmentright;
    final TokenFinder<Assignment> funcdec;
    final TokenFinder<Assignment> localdec;
    final TokenFinder<StatementLike> globaldec;
    final TokenFinder<String> varargparam;
    final TokenFinder<FunctionDefinition> funcexpr;
    final TokenFinder<FunctionDefinition> funcbody;
    final TokenFinder<Map.Entry<@Nullable Expression, Expression>> tablefield;
    final TokenFinder<TableConstructor> tableconstructor;

    block = TokenFinder.zeroOrMore(() -> stmt.map((tokenFinder, statements) -> {
      statements.forEach(tokenFinder.block::pushStatement);
      return statements;
    }));

    s_end = TokenFinder.keyword(KeywordTokenType.END)
        .throwing("expected statement or \"end\"")
        .consume();

    final TokenFinder<If.Section> if_section = TokenFinder.ordered(
        () -> expr.map((tokenFinder, expressions) ->
            List.of(new If.Section(tokenFinder.block = new Block(tokenFinder.block, true), expressions.getFirst()))
        ).throwing("expected condition")
            .withParentsTakeContext(),
        () -> TokenFinder.keyword(KeywordTokenType.THEN).consume()
            .throwing("expected \"then\" after condition"),
        () -> block.consume()
    );

    s_if = TokenFinder.ordered(
        () -> (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.IF),
        () -> (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ordered(
            () -> if_section,
            () -> TokenFinder.zeroOrMore(() -> TokenFinder.ordered(
                () -> (TokenFinder<If.Section>)TokenFinder.keyword(KeywordTokenType.ELSEIF).consume(),
                () -> if_section
            ))
        ).group(),
        () -> (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.optional(TokenFinder.ordered(
            () -> TokenFinder.keyword(KeywordTokenType.ELSE)
                .map((tokenFinder, keywords) ->
                    List.of(tokenFinder.block = (Block)new Block(tokenFinder.block, true).at(keywords.getFirst()))
                ).withParentsTakeContext(),
            () -> (TokenFinder<Block>)block.consume()
        ))
    ).map((tokenFinder, objects) -> {
      Token.Keyword keyword = (Token.Keyword)objects.getFirst();
      List<If.Section> ifSections = (List<If.Section>)objects.get(1);
      Block elseSection = null;
      if (objects.size() > 2) {
        elseSection = (Block)objects.get(2);
      }
      return List.of((If)new If(ifSections, elseSection, tokenFinder.block).at(keyword));
    });

    s_do = TokenFinder.ordered(
        () -> TokenFinder.keyword(KeywordTokenType.DO).map((tokenFinder, keywords) -> {
          Do s = (Do)new Do(tokenFinder.block).at(keywords.getFirst());
          tokenFinder.block = s.body;
          return List.of(s); // return now, add statements afterward
        }).withParentsTakeContext(),
        () -> (TokenFinder<Do>)block.consume(),
        () -> (TokenFinder<Do>)s_end
    ).map((tokenFinder, dos) -> List.of(dos.getFirst()));

    s_while = TokenFinder.ordered(
        () -> TokenFinder.ordered(
            () -> (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.WHILE),
            () -> (TokenFinder<Object>)(TokenFinder<?>)expr.throwing("expected condition")
        ).map((tokenFinder, keywords) -> {
          RepeatWhile s = (RepeatWhile)new RepeatWhile((Expression)keywords.getLast(), tokenFinder.block).at((Token)keywords.getFirst());
          tokenFinder.block = s.body;
          return List.of(s);
        }).withParentsTakeContext(),
        () -> (TokenFinder<RepeatWhile>)TokenFinder.keyword(KeywordTokenType.DO).consume().throwing("expected \"do\""),
        () -> (TokenFinder<RepeatWhile>)block.consume(),
        () -> (TokenFinder<RepeatWhile>)s_end
    );

    s_repeat = TokenFinder.ordered(
        () -> (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.REPEAT)
            .map((tokenFinder, keywords) ->
                List.of(tokenFinder.block = new Block((Block)new Block(tokenFinder.block, true).enableBreak().at(keywords.getFirst())))
            ).withParentsTakeContext(),
        () -> (TokenFinder<Object>)block.consume(),
        () -> (TokenFinder<Object>)TokenFinder.keyword(KeywordTokenType.UNTIL).consume()
            .throwing("expected statement or \"until\""),
        () -> (TokenFinder<Object>)(TokenFinder<?>)expr
            .throwing("expected expression")
    ).map((tokenFinder, objects) -> {
      Block repeatBlock = (Block)objects.getFirst();
      Expression untilExpression = (Expression)objects.getLast();
      return List.of(new RepeatUntil(untilExpression, repeatBlock, true));
    });

  }
}
