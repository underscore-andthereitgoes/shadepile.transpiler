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
    final TokenFinder<Statement>[] block = new TokenFinder[1];
    final TokenFinder<?>[] s_end = new TokenFinder[1];
    final TokenFinder<If>[] s_if = new TokenFinder[1];
    final TokenFinder<Do>[] s_do = new TokenFinder[1];
    final TokenFinder<RepeatWhile>[] s_while = new TokenFinder[1];
    final TokenFinder<RepeatUntil>[] s_repeat = new TokenFinder[1];
    final TokenFinder<For>[] s_for = new TokenFinder[1];
    final TokenFinder<ForNumeric>[] s_fornum = new TokenFinder[1];
    final TokenFinder<ForGeneric>[] s_forgen = new TokenFinder[1];
    final TokenFinder<Goto>[] s_goto = new TokenFinder[1];
    final TokenFinder<Break>[] s_break = new TokenFinder[1];
    final TokenFinder<Return>[] s_return = new TokenFinder[1];
    final TokenFinder<Statement>[] stmt = new TokenFinder[1];
    final TokenFinder<Label>[] label = new TokenFinder[1];
    final TokenFinder<UnaryOperator>[] unop = new TokenFinder[1];
    final TokenFinder<BinaryOperator>[] binop = new TokenFinder[1];
    final TokenFinder<UnaryOperatorOrExpression>[] unopexpr = new TokenFinder[1];
    final TokenFinder<Expression>[] opexpr = new TokenFinder[1];
    final TokenFinder<Expression>[] term = new TokenFinder[1];
    final TokenFinder<Expression>[] groupexpr = new TokenFinder[1];
    final TokenFinder<Expression>[] expr = new TokenFinder[1];
    final TokenFinder<String>[] namelist = new TokenFinder[1];
    final TokenFinder<?>[] attr = new TokenFinder[1];
    final TokenFinder<String>[] attrnamelist = new TokenFinder[1];
    final TokenFinder<CallInfo>[] callpath = new TokenFinder[1];
    final TokenFinder<VariableOrPropertyAccess>[] props = new TokenFinder[1];
    final TokenFinder<Expression>[] propsfunccall = new TokenFinder[1];
    final TokenFinder<FunctionCall>[] funccall = new TokenFinder[1];
    final TokenFinder<@Nullable String>[] methodsuff = new TokenFinder[1];
    final TokenFinder<CallInfo>[] funccallsuff = new TokenFinder[1];
    final TokenFinder<PropertyPath>[] propertypathsuff = new TokenFinder[1];
    final TokenFinder<VariableOrPropertyAccess>[] simplevariable = new TokenFinder[1];
    final TokenFinder<VariableOrPropertyAccess>[] varlist = new TokenFinder[1];
    final TokenFinder<Assignment>[] assignment = new TokenFinder[1];
    final TokenFinder<Expression>[] assignmentright = new TokenFinder[1];
    final TokenFinder<Assignment>[] funcdec = new TokenFinder[1];
    final TokenFinder<Assignment>[] localdec = new TokenFinder[1];
    final TokenFinder<StatementLike>[] globaldec = new TokenFinder[1];
    final TokenFinder<String>[] varargparam = new TokenFinder[1];
    final TokenFinder<FunctionDefinition>[] funcexpr = new TokenFinder[1];
    final TokenFinder<FunctionDefinition>[] funcbody = new TokenFinder[1];
    final TokenFinder<Map.Entry<@Nullable Expression, Expression>>[] tablefield = new TokenFinder[1];
    final TokenFinder<TableConstructor>[] tableconstructor = new TokenFinder[1];

    block[0] = TokenFinder.zeroOrMore(() -> stmt[0].map((tokenFinder, statements) -> {
      statements.forEach(tokenFinder.block::pushStatement);
      return statements;
    }));

    s_end[0] = TokenFinder.keyword(KeywordTokenType.END)
        .throwing("expected statement or \"end\"")
        .consume();

    final TokenFinder<If.Section> if_section = TokenFinder.ordered(
        () -> expr[0].map((tokenFinder, expressions) ->
            List.of(new If.Section(tokenFinder.block = new Block(tokenFinder.block, true), expressions.getFirst()))
        ).throwing("expected condition")
            .withParentsTakeContext(),
        (TokenFinder<If.Section>)TokenFinder.keyword(KeywordTokenType.THEN).consume()
            .throwing("expected \"then\" after condition"),
        (TokenFinder<If.Section>)block[0].consume()
    );

    s_if[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.IF),
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ordered(
            if_section,
            TokenFinder.zeroOrMore(() -> TokenFinder.ordered(
                (TokenFinder<If.Section>)TokenFinder.keyword(KeywordTokenType.ELSEIF).consume(),
                if_section
            ))
        ).group(),
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.optional(TokenFinder.ordered(
            TokenFinder.keyword(KeywordTokenType.ELSE)
                .map((tokenFinder, keywords) ->
                    List.of(tokenFinder.block = (Block)new Block(tokenFinder.block, true).at(keywords.getFirst()))
                ).withParentsTakeContext(),
            (TokenFinder<Block>)block[0].consume()
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

    s_do[0] = TokenFinder.ordered(
        TokenFinder.keyword(KeywordTokenType.DO).map((tokenFinder, keywords) -> {
          Do s = (Do)new Do(tokenFinder.block).at(keywords.getFirst());
          tokenFinder.block = s.body;
          return List.of(s); // return now, add statements afterward
        }).withParentsTakeContext(),
        (TokenFinder<Do>)block[0].consume(),
        (TokenFinder<Do>)s_end[0]
    ).map((tokenFinder, dos) -> List.of(dos.getFirst()));

    s_while[0] = TokenFinder.ordered(
        TokenFinder.ordered(
            (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.WHILE),
            () -> (TokenFinder<Object>)(TokenFinder<?>)expr[0].throwing("expected condition")
        ).map((tokenFinder, keywords) -> {
          RepeatWhile s = (RepeatWhile)new RepeatWhile((Expression)keywords.getLast(), tokenFinder.block).at((Token)keywords.getFirst());
          tokenFinder.block = s.body;
          return List.of(s);
        }).withParentsTakeContext(),
        (TokenFinder<RepeatWhile>)TokenFinder.keyword(KeywordTokenType.DO).consume().throwing("expected \"do\""),
        (TokenFinder<RepeatWhile>)block[0].consume(),
        (TokenFinder<RepeatWhile>)s_end[0]
    );

    s_repeat[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.REPEAT)
            .map((tokenFinder, keywords) ->
                List.of(tokenFinder.block = new Block((Block)new Block(tokenFinder.block, true).enableBreak().at(keywords.getFirst())))
            ).withParentsTakeContext(),
        (TokenFinder<Object>)block[0].consume(),
        (TokenFinder<Object>)TokenFinder.keyword(KeywordTokenType.UNTIL).consume()
            .throwing("expected statement or \"until\""),
        (TokenFinder<Object>)(TokenFinder<?>)expr[0]
            .throwing("expected expression")
    ).map((tokenFinder, objects) -> {
      Block repeatBlock = (Block)objects.getFirst();
      Expression untilExpression = (Expression)objects.getLast();
      return List.of(new RepeatUntil(untilExpression, repeatBlock, true));
    });

    s_for[0] = TokenFinder.ordered(
        (TokenFinder<For>)TokenFinder.keyword(KeywordTokenType.FOR).consume(),
        TokenFinder.firstValid(
            () -> (TokenFinder<For>)(TokenFinder<?>)s_fornum[0],
            () -> (TokenFinder<For>)(TokenFinder<?>)s_forgen[0],
            (TokenFinder<For>)TokenFinder.invalid().throwing("expected generic (variables in values) or numeric (variable = start, stop[, step]) initializer after \"for\"")
        ).withParentsTakeContext(),
        (TokenFinder<For>)TokenFinder.keyword(KeywordTokenType.DO).consume().throwing("expected \"do\""),
        (TokenFinder<For>)block[0].consume(),
        (TokenFinder<For>)s_end[0]
    );

    s_fornum[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ofClass(Token.Name.class),
        (TokenFinder<Object>)TokenFinder.ofClass(Token.SingleEquals.class).consume(),
        () -> (TokenFinder<Object>)(TokenFinder<?>)expr[0].throwing("expected start after \"for variable =\""),
        (TokenFinder<Object>)TokenFinder.ofClass(Token.Comma.class).consume().throwing("expected comma after \"for variable = start\""),
        () -> (TokenFinder<Object>)(TokenFinder<?>)expr[0].throwing("expected stop after \"for variable = start,\""),
        TokenFinder.optional(TokenFinder.ordered(
          (TokenFinder<Object>)TokenFinder.ofClass(Token.Comma.class).consume(),
          () -> (TokenFinder<Object>)(TokenFinder<?>)expr[0].throwing("expected step after \"for variable = start, stop,\"")
        ))
    ).map((tokenFinder, objects) -> {
      Token.Name variableNameToken = (Token.Name)objects.getFirst();
      ForNumeric s = new ForNumeric(variableNameToken.text, objects.subList(0, objects.size()).stream().map(o -> (Expression)o).toArray(Expression[]::new), tokenFinder.block);
      tokenFinder.block = s.body;
      return List.of(s);
    }).withParentsTakeContext();

  }
}
