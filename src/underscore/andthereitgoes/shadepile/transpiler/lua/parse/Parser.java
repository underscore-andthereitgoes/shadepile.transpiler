package underscore.andthereitgoes.shadepile.transpiler.lua.parse;

import org.apache.commons.lang3.function.Functions;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.*;
import underscore.andthereitgoes.shadepile.transpiler.lua.BinaryOperator;
import underscore.andthereitgoes.shadepile.transpiler.lua.UnaryOperator;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.NewlineCountingStringBuilder;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.*;

import java.util.*;
import java.util.function.*;


public class Parser {

  private static abstract class CallInfoOrPropertyPath {}

  private static class CallInfo extends CallInfoOrPropertyPath {
    public final @Nullable String method;
    public final @NotNull List<@NotNull Expression> arguments;
    public final PositionedNode at;

    private CallInfo(@Nullable String method, @NotNull List<@NotNull Expression> arguments, Token at) {
      this(method, arguments, new PositionedNode() {
        @Override
        public void emit(NewlineCountingStringBuilder builder) { throw new UnsupportedOperationException(); }
      }.at(at));
    }

    private CallInfo(@Nullable String method, @NotNull List<@NotNull Expression> arguments, PositionedNode at) {
      this.method = method;
      this.arguments = arguments;
      this.at = at;
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
    final TokenFinder<StatementLike>[] block = new TokenFinder[1];
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
    final TokenFinder<Label>[] label = new TokenFinder[1];
    final TokenFinder<StatementLike>[] stmt = new TokenFinder[1];
    final TokenFinder<UnaryOperator>[] unop = new TokenFinder[1];
    final TokenFinder<BinaryOperator>[] binop = new TokenFinder[1];
    final TokenFinder<UnaryOperatorOrExpression>[] unopexpr = new TokenFinder[1];
    final TokenFinder<Expression>[] opexpr = new TokenFinder[1];
    final TokenFinder<Expression>[] term = new TokenFinder[1];
    final TokenFinder<Expression>[] groupexpr = new TokenFinder[1];
    final TokenFinder<Expression>[] expr = new TokenFinder[1];
    final TokenFinder<Expression>[] exprlist = new TokenFinder[1];
    final TokenFinder<String>[] name = new TokenFinder[1];
    final TokenFinder<String>[] namelist = new TokenFinder[1];
    final TokenFinder<?>[] attr = new TokenFinder[1];
    final TokenFinder<String>[] attrnamelist = new TokenFinder[1];
    final TokenFinder<CallInfoOrPropertyPath>[] callpath = new TokenFinder[1]; // property access and/or function calls
    final TokenFinder<Expression>[] propsfunccall = new TokenFinder[1]; // property path or function call; also includes single grouped expressions
    final TokenFinder<VariableOrPropertyAccess>[] props = new TokenFinder[1]; // property path only
    final TokenFinder<FunctionCall>[] funccall = new TokenFinder[1]; // function call only
    final TokenFinder<@Nullable String>[] methodsuff = new TokenFinder[1];
    final TokenFinder<CallInfo>[] funccallsuff = new TokenFinder[1];
    final TokenFinder<PropertyPath>[] propertypathsuff = new TokenFinder[1];
    final TokenFinder<VariableOrPropertyAccess>[] simplevariable = new TokenFinder[1];
    final TokenFinder<VariableOrPropertyAccess>[] varlist = new TokenFinder[1];
    final TokenFinder<Object>[] assignmentright = new TokenFinder[1];
    final TokenFinder<Assignment>[] assignment = new TokenFinder[1];
    final TokenFinder<String>[] varargparam = new TokenFinder[1];
    final TokenFinder<FunctionDefinition>[] funcexpr = new TokenFinder[1];
    final TokenFinder<FunctionDefinition>[] funcbody = new TokenFinder[1];
    final TokenFinder<Assignment>[] funcdec = new TokenFinder[1];
    final TokenFinder<Assignment>[] localdec = new TokenFinder[1];
    final TokenFinder<StatementLike>[] globaldec = new TokenFinder[1];
    final TokenFinder<Map.Entry<@Nullable Expression, Expression>>[] tablefield = new TokenFinder[1];
    final TokenFinder<TableConstructor>[] tableconstructor = new TokenFinder[1];

    block[0] = TokenFinder.zeroOrMore(() -> stmt[0].map((tokenFinder, statements) -> {
      if (statements.size() != 1) throw new IllegalStateException();
      tokenFinder.block.pushStatement(statements.getFirst());
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
      return List.of((RepeatUntil)new RepeatUntil(untilExpression, repeatBlock, true).at(untilExpression));
    });

    s_for[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.FOR),
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.firstValid(
            () -> (TokenFinder<For>)(TokenFinder<?>)s_fornum[0],
            () -> (TokenFinder<For>)(TokenFinder<?>)s_forgen[0],
            (TokenFinder<For>)TokenFinder.invalid().throwing("expected generic (variables in values) or numeric (variable = start, stop[, step]) initializer after \"for\"")
        ).withParentsTakeContext(),
        (TokenFinder<Object>)TokenFinder.keyword(KeywordTokenType.DO).consume().throwing("expected \"do\""),
        (TokenFinder<Object>)block[0].consume(),
        (TokenFinder<Object>)s_end[0]
    ).map((tokenFinder, objects) ->
        List.of((For)((For)objects.getLast()).at((Token)objects.getFirst()))
    );

    s_fornum[0] = TokenFinder.ordered(
        () -> (TokenFinder<Object>)(TokenFinder<?>)name[0],
        (TokenFinder<Object>)TokenFinder.ofClass(Token.SingleEquals.class).consume(),
        () -> (TokenFinder<Object>)(TokenFinder<?>)expr[0].throwing("expected start after \"for variable =\""),
        (TokenFinder<Object>)TokenFinder.ofClass(Token.Comma.class).consume().throwing("expected comma after \"for variable = start\""),
        () -> (TokenFinder<Object>)(TokenFinder<?>)expr[0].throwing("expected stop after \"for variable = start,\""),
        TokenFinder.optional(TokenFinder.ordered(
          (TokenFinder<Object>)TokenFinder.ofClass(Token.Comma.class).consume(),
          () -> (TokenFinder<Object>)(TokenFinder<?>)expr[0].throwing("expected step after \"for variable = start, stop,\"")
        ))
    ).map((tokenFinder, objects) -> {
      ForNumeric s = new ForNumeric((String)objects.getFirst(), objects.subList(1, objects.size()).stream().map(o -> (Expression)o).toArray(Expression[]::new), tokenFinder.block);
      tokenFinder.block = s.body;
      return List.of(s);
    }).withParentsTakeContext();

    s_forgen[0] = TokenFinder.ordered(
        () -> (TokenFinder<Object>)(TokenFinder<?>)namelist[0].group(),
        (TokenFinder<Object>)TokenFinder.keyword(KeywordTokenType.IN).consume(),
        () -> (TokenFinder<Object>)(TokenFinder<?>)exprlist[0].throwing("expected expression or expression list after \"for variables in\"").group()
    ).map((tokenFinder, objects) -> {
      List<String> names = (List<String>)objects.getFirst();
      List<Expression> expressions = (List<Expression>)objects.getLast();
      ForGeneric s = new ForGeneric(names.toArray(String[]::new), expressions.toArray(Expression[]::new), tokenFinder.block);
      tokenFinder.block = s.body;
      return List.of(s);
    }).withParentsTakeContext();

    s_goto[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.GOTO),
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ofClass(Token.Name.class).throwing("expected name after \"goto\"")
    ).map((tokenFinder, objects) ->
        List.of((Goto)new Goto(((Token.Name)objects.getLast()).text).at((Token)objects.getFirst()))
    );

    s_break[0] = TokenFinder.keyword(KeywordTokenType.BREAK).map((tokenFinder, keywords) -> List.of((Break)new Break().at(keywords.getFirst())));

    s_return[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.RETURN),
        () -> (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.optional(exprlist[0]).group(),
        (TokenFinder<Object>)TokenFinder.optional(TokenFinder.ofClass(Token.Semicolon.class)).consume()
    ).map((tokenFinder, objects) ->
        List.of((Return)new Return(((List<Expression>)objects.getLast()).toArray(Expression[]::new)).at((Token)objects.getFirst()))
    );

    label[0] = TokenFinder.ordered(
        (TokenFinder<Label>)TokenFinder.ofClass(Token.LabelBoundary.class).consume(),
        TokenFinder.ofClass(Token.Name.class).map((tokenFinder, names) -> List.of((Label)new Label(names.getFirst().text).at(names.getFirst()))),
        (TokenFinder<Label>)TokenFinder.ofClass(Token.LabelBoundary.class).consume().throwing("expected :: after label name")
    );

    stmt[0] = TokenFinder.firstValid(
        TokenFinder.ofClass(Token.Semicolon.class).map((tokenFinder, tokens) -> List.of((Empty)new Empty().at(tokens.getFirst()))),
        (TokenFinder<StatementLike>)(TokenFinder<?>)label[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_do[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_if[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_for[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_while[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_repeat[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_goto[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_break[0],
        (TokenFinder<StatementLike>)(TokenFinder<?>)s_return[0],
        () -> (TokenFinder<StatementLike>)(TokenFinder<?>)funcdec[0],
        () -> globaldec[0],
        () -> (TokenFinder<StatementLike>)(TokenFinder<?>)localdec[0],
        () -> (TokenFinder<StatementLike>)(TokenFinder<?>)assignment[0],
        () -> (TokenFinder<StatementLike>)(TokenFinder<?>)funccall[0]
    );

    unop[0] = TokenFinder.ofClass(Token.Operator.class).map((tokenFinder, operators) -> {
      UnaryOperator unEq = operators.getFirst().type.unaryEquivalent;
      return unEq != null ? List.of(unEq) : null;
    });
    binop[0] = TokenFinder.ofClass(Token.Operator.class).map((tokenFinder, operators) -> {
      BinaryOperator binEq = operators.getFirst().type.binaryEquivalent;
      return binEq != null ? List.of(binEq) : null;
    });

    unopexpr[0] = TokenFinder.ordered(
        (TokenFinder<UnaryOperatorOrExpression>)(TokenFinder<?>)TokenFinder.zeroOrMore(unop[0]),
        () -> (TokenFinder<UnaryOperatorOrExpression>)(TokenFinder<?>)term[0]
    );

    opexpr[0] = TokenFinder.ordered(
        (TokenFinder<OperatorOrExpression>)(TokenFinder<?>)unopexpr[0],
        TokenFinder.zeroOrMore(TokenFinder.ordered(
            (TokenFinder<OperatorOrExpression>)(TokenFinder<?>)binop[0],
            (TokenFinder<OperatorOrExpression>)(TokenFinder<?>)unopexpr[0]
                .throwing("expected expression after operator")
        ))
    ).map((tokenFinder, opExps) -> {
      final ArrayList<OperatorOrExpression> parts = new ArrayList<>(opExps);

      final IntConsumer[] collapseUnaryOperation = new IntConsumer[1];
      collapseUnaryOperation[0] = (index) -> {
        OperatorOrExpression part = parts.get(index);
        if (part instanceof UnaryOperator operator) {
          OperatorOrExpression partAfter = parts.get(index + 1);
          if (partAfter instanceof UnaryOperator) {
            collapseUnaryOperation[0].accept(index + 1);
          } else if (partAfter instanceof Expression operand) {
            parts.remove(index);
            parts.set(index, new UnaryOperation(operator, operand));
          } else throw new IllegalStateException();
        }
      };

      for (OperatorTokenType.ParseOrderItem parseOrderItem: OperatorTokenType.order) {
        int start = 0, stop = parts.size() - 1, step = 1;
        if (parseOrderItem.reverse) {
          start = stop;
          stop = 0;
          step = -1;
        }
        for (int i = start; i != stop; i += step) {
          if (i < 0 || i >= parts.size()) break;
          OperatorOrExpression part = parts.get(i);
          if (parseOrderItem instanceof OperatorTokenType.ParseOrderItemBinary orderItemBinary) {
            if (part instanceof BinaryOperator operator) {
              if (orderItemBinary.operators.contains(part)) {
                collapseUnaryOperation[0].accept(i + 1);
                Expression leftOperand = (Expression)parts.get(i - 1);
                Expression rightOperand = (Expression)parts.get(i + 1);
                parts.remove(i - 1);
                parts.remove(i - 1);
                parts.set(i - 1, new BinaryOperation(leftOperand, rightOperand, operator));
                i--;
              }
            }
          } else if (parseOrderItem instanceof OperatorTokenType.ParseOrderItemUnary orderItemUnary) {
            if (part instanceof UnaryOperator) {
              if (orderItemUnary.operators.contains(part)) {
                collapseUnaryOperation[0].accept(i);
              }
            }
          }
        }
      }

      if (parts.size() != 1) throw new IllegalStateException();
      if (!(parts.getFirst() instanceof Expression)) throw new IllegalStateException();

      return (List<Expression>)(List<?>)parts;
    });

    term[0] = TokenFinder.firstValid(
        TokenFinder.ofClass(Token.NilLiteral.class).map((tokenFinder, nilLiterals) -> List.of((Literal.NilLiteral)new Literal.NilLiteral().at(nilLiterals.getFirst()))),
        TokenFinder.ofClass(Token.BooleanLiteral.class).map((tokenFinder, booleanLiterals) -> List.of((Literal.BooleanLiteral)new Literal.BooleanLiteral(booleanLiterals.getFirst().value).at(booleanLiterals.getFirst()))),
        TokenFinder.ofClass(Token.NumberLiteral.class).map((tokenFinder, numberLiterals) -> {
          Token.NumberLiteral lit = numberLiterals.getFirst();
          Number value = lit.value;
          if (value instanceof Double d) return List.of((Literal.FloatLiteral)new Literal.FloatLiteral(d).at(lit));
          else if (value instanceof Long l) return List.of((Literal.IntegerLiteral)new Literal.IntegerLiteral(l).at(lit));
          else throw new IllegalStateException();
        }),
        TokenFinder.ofClass(Token.StringLiteral.class).map((tokenFinder, stringLiterals) -> List.of(new Literal.StringLiteral(stringLiterals.getFirst().value))),
        TokenFinder.ofClass(Token.ThreeDots.class).map((tokenFinder, threeDots) -> List.of(Optional.ofNullable((Expression)tokenFinder.block.findVariable("...")).orElseThrow(() -> new LuaParseError("... is not defined in this scope")))),
        () -> (TokenFinder<Expression>)(TokenFinder<?>)tableconstructor[0],
        () -> propsfunccall[0],
        () -> (TokenFinder<Expression>)(TokenFinder<?>)funcexpr[0]
    );

    groupexpr[0] = TokenFinder.ordered(
        (TokenFinder<Expression>)TokenFinder.ofClassAndAlso(Token.Parenthesis.class, parenthesis -> parenthesis.side == Token.BracketSide.LEFT).consume(),
        () -> expr[0].throwing("expected expression after opening parenthesis"),
        (TokenFinder<Expression>)TokenFinder.ofClassAndAlso(Token.Parenthesis.class, parenthesis -> parenthesis.side == Token.BracketSide.RIGHT).consume().throwing("expected a closing parenthesis")
    ).map((tokenFinder, expressions) -> List.of((GroupingExpression)new GroupingExpression(expressions.getFirst()).at(expressions.getFirst())));

    expr[0] = opexpr[0]; // in the TypeScript version, logical operations are split

    exprlist[0] = TokenFinder.ordered(
        expr[0],
        TokenFinder.zeroOrMore(TokenFinder.ordered(
            (TokenFinder<Expression>)TokenFinder.ofClass(Token.Comma.class).consume(),
            expr[0].throwing("expected expression after comma")
        ))
    );

    name[0] = TokenFinder.ofClass(Token.Name.class).map((tokenFinder, names) -> List.of(names.getFirst().text));

    namelist[0] = TokenFinder.ordered(
        name[0],
        TokenFinder.zeroOrMore(TokenFinder.ordered(
            (TokenFinder<String>)TokenFinder.ofClass(Token.Comma.class).consume(),
            name[0]
                .throwing("expected name after comma")
        ))
    );

    attr[0] = TokenFinder.ordered(
        (TokenFinder<Void>)TokenFinder.ofClassAndAlso(Token.Operator.class, operator -> operator.type == OperatorTokenType.LT).consume(),
        (TokenFinder<Void>)TokenFinder.ofClass(Token.Name.class).consume(),
        (TokenFinder<Void>)TokenFinder.ofClassAndAlso(Token.Operator.class, operator -> operator.type == OperatorTokenType.GT).consume()
    ).consume();

    attrnamelist[0] = TokenFinder.ordered(
        (TokenFinder<String>)TokenFinder.optional(attr[0]),
        name[0],
        (TokenFinder<String>)TokenFinder.optional(attr[0]),
        TokenFinder.zeroOrMore(TokenFinder.ordered(
            (TokenFinder<String>)TokenFinder.ofClass(Token.Comma.class).consume(),
            name[0],
            (TokenFinder<String>)TokenFinder.optional(attr[0])
        ))
    );

    callpath[0] = TokenFinder.ordered(
        TokenFinder.firstValid(
            () -> simplevariable[0].map((tokenFinder, list) -> List.of(new PropertyPath(list.getFirst()))),
            () -> groupexpr[0].map((tokenFinder, list) -> List.of(new PropertyPath(list.getFirst())))
        ),
        TokenFinder.zeroOrMore(TokenFinder.firstValid(
            () -> (TokenFinder<CallInfoOrPropertyPath>)(TokenFinder<?>)propertypathsuff[0],
            () -> (TokenFinder<CallInfoOrPropertyPath>)(TokenFinder<?>)funccallsuff[0]
        ))
    );

    final TriFunction<List<CallInfoOrPropertyPath>, Boolean, Boolean, Expression> callpathmapper = (callInfoOrPropertyPaths, allowPath, allowCall) -> {
      if (callInfoOrPropertyPaths.isEmpty() || !(callInfoOrPropertyPaths.getFirst() instanceof PropertyPath baseExpressionPath)) throw new IllegalStateException();
      Expression expression = baseExpressionPath.item;
      for (int i = 1, numPieces = callInfoOrPropertyPaths.size(); i < numPieces; i++) {
        CallInfoOrPropertyPath piece = callInfoOrPropertyPaths.get(i);
        expression = switch (piece) {
          case CallInfo callInfo ->
              (FunctionCall)new FunctionCall(expression, callInfo.method, callInfo.arguments.toArray(Expression[]::new)).at(callInfo.at);
          case PropertyPath propertyPath ->
              new PropertyAccess(expression, propertyPath.item);
          case null, default -> throw new IllegalStateException();
        };
      }
      if (allowCall && allowPath) return expression;
      if (allowCall) {
        if (expression instanceof FunctionCall) return expression;
      } else if (allowPath) {
        if (expression instanceof VariableOrPropertyAccess) return expression;
      } else throw new IllegalStateException();
      return null;
    };

    final BiFunction<TokenFinder<?>, List<CallInfoOrPropertyPath>, List<Expression>> propsfunccallmapper = (_, callInfoOrPropertyPaths) -> List.of(callpathmapper.apply(callInfoOrPropertyPaths, true, true));
    final BiFunction<TokenFinder<?>, List<CallInfoOrPropertyPath>, List<VariableOrPropertyAccess>> propsmapper = (_, callInfoOrPropertyPaths) -> List.of((VariableOrPropertyAccess)callpathmapper.apply(callInfoOrPropertyPaths, true, false));
    final BiFunction<TokenFinder<?>, List<CallInfoOrPropertyPath>, List<FunctionCall>> funccallmapper = (_, callInfoOrPropertyPaths) -> List.of((FunctionCall)callpathmapper.apply(callInfoOrPropertyPaths, false, true));

    propsfunccall[0] = callpath[0].map(propsfunccallmapper);
    props[0] = callpath[0].map(propsmapper);
    funccall[0] = callpath[0].map(funccallmapper);

    methodsuff[0] = TokenFinder.firstValid(
        TokenFinder.ordered(
            (TokenFinder<String>)TokenFinder.ofClass(Token.PropertyColon.class).consume(),
            name[0].throwing("expected method name after colon")
        ),
        TokenFinder.constantValid(Collections.singletonList(null))
    );

    funccallsuff[0] = TokenFinder.oneOrMore(TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)methodsuff[0],
        TokenFinder.firstValid(
            TokenFinder.ordered(
                (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ofClassAndAlso(Token.Parenthesis.class, parenthesis -> parenthesis.side == Token.BracketSide.LEFT),
                (TokenFinder<Object>)(TokenFinder<?>)exprlist[0].throwing("expected parameter list after opening parenthesis"),
                (TokenFinder<Object>)TokenFinder.ofClassAndAlso(Token.Parenthesis.class, parenthesis -> parenthesis.side == Token.BracketSide.RIGHT).consume().throwing("expected a closing parenthesis")
            ),
            (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ofClass(Token.StringLiteral.class).map((tokenFinder, stringLiterals) -> List.of((Literal.StringLiteral)new Literal.StringLiteral(stringLiterals.getFirst().value).at(stringLiterals.getFirst()))),
            () -> (TokenFinder<Object>)(TokenFinder<?>)tableconstructor[0]
        )
    ).map((tokenFinder, objects) -> {
      if (objects.size() < 2) throw new IllegalStateException();
      String method = (String)objects.getFirst();
      Object firstThing = objects.get(1);
      if (firstThing instanceof Token.Parenthesis token) {
        return List.of(new CallInfo(method, (List<Expression>)(List<?>)objects.subList(2, objects.size()), token));
      } else if (firstThing instanceof Expression onlyArgument) {
        return List.of(new CallInfo(method, List.of(onlyArgument), onlyArgument));
      } else throw new IllegalStateException();
    }));

    propertypathsuff[0] = TokenFinder.oneOrMore(TokenFinder.firstValid(
        TokenFinder.ordered(
            (TokenFinder<Expression>)TokenFinder.ofClassAndAlso(Token.SquareBracket.class, squareBracket -> squareBracket.side == Token.BracketSide.LEFT).consume(),
            () -> expr[0],
            (TokenFinder<Expression>)TokenFinder.ofClassAndAlso(Token.SquareBracket.class, squareBracket -> squareBracket.side == Token.BracketSide.RIGHT).consume()
        ),
        (TokenFinder<Expression>)(TokenFinder<?>)TokenFinder.ordered(
            (TokenFinder<Literal.StringLiteral>)TokenFinder.ofClass(Token.PropertyDot.class).consume(),
            TokenFinder.ofClass(Token.Name.class)
                .map((tokenFinder, names) -> List.of((Literal.StringLiteral)new Literal.StringLiteral(names.getFirst().text).at(names.getFirst())))
                .throwing("expected name after property dot")
        )
    ).map((tokenFinder, expressions) -> List.of(new PropertyPath(expressions.getFirst()))));

    simplevariable[0] = TokenFinder.ofClass(Token.Name.class)
        .map((tokenFinder, names) -> List.of((VariableOrPropertyAccess)tokenFinder.block.findVariableOrGlobal(names.getFirst().text).at(names.getFirst())))
    ;

    varlist[0] = TokenFinder.ordered(
        props[0],
        TokenFinder.oneOrMore(TokenFinder.ordered(
            (TokenFinder<VariableOrPropertyAccess>)TokenFinder.ofClass(Token.Comma.class).consume(),
            props[0].throwing("expected variable after comma")
        ))
    );

    final BiFunction<TokenFinder<?>, List<Object>, List<Assignment>> assignmentmapper = (_, objects) -> {
      if (objects.size() != 3) throw new IllegalStateException();
      List<VariableOrPropertyAccess> targets = (List<VariableOrPropertyAccess>)objects.getFirst();
      Token equalSign = (Token)objects.get(1);
      List<Expression> values = (List<Expression>)objects.getLast();
      return List.of((Assignment)new Assignment(targets.toArray(VariableOrPropertyAccess[]::new), values.toArray(Expression[]::new)).at(equalSign));
    };

    assignmentright[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ofClass(Token.SingleEquals.class),
        (TokenFinder<Object>)(TokenFinder<?>)exprlist[0]
    );

    assignment[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)varlist[0],
        assignmentright[0]
    ).map(assignmentmapper);

    varargparam[0] = TokenFinder.ordered(
        TokenFinder.ofClass(Token.ThreeDots.class).map((tokenFinder, threeDots) -> List.of("...")),
        TokenFinder.optional(TokenFinder.ofClass(Token.Name.class).map((tokenFinder, names) -> List.of(names.getFirst().text)))
    ).map((tokenFinder, strings) -> List.of(String.join("", strings)));

    funcexpr[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.FUNCTION).map((tokenFinder, keywords) -> {
          tokenFinder.context.put("method", false);
          return keywords;
        }).withParentsTakeContext(),
        () -> (TokenFinder<Object>)(TokenFinder<?>)funcbody[0].throwing("expected function body after \"function\"")
    ).map((tokenFinder, objects) -> {
      Token.Keyword kw = (Token.Keyword)objects.getFirst();
      FunctionDefinition body = (FunctionDefinition)objects.getLast();
      return List.of((FunctionDefinition)body.at(kw));
    });

    funcbody[0] = TokenFinder.ordered(
        TokenFinder.ordered(
            (TokenFinder<String>)TokenFinder.ofClassAndAlso(Token.Parenthesis.class, parenthesis -> parenthesis.side == Token.BracketSide.LEFT).consume().throwing("expected opening parenthesis for function"),
            TokenFinder.firstValid(
                TokenFinder.ordered(
                    namelist[0],
                    TokenFinder.optional(TokenFinder.ordered(
                        (TokenFinder<String>)TokenFinder.ofClass(Token.Comma.class).consume(),
                        varargparam[0]
                    ))
                ),
                varargparam[0],
                (TokenFinder<String>)TokenFinder.emptyValid()
            ),
            (TokenFinder<String>)TokenFinder.ofClassAndAlso(Token.Parenthesis.class, parenthesis -> parenthesis.side == Token.BracketSide.RIGHT).consume().throwing("expected closing parenthesis after parameters")
        ).map((tokenFinder, parameterNames) -> {
          boolean method = Optional.ofNullable((Boolean)tokenFinder.context.get("method")).orElse(false);
          FunctionDefinition funcDef = new FunctionDefinition(parameterNames.toArray(String[]::new), method, tokenFinder.block);
          tokenFinder.block = funcDef.body;
          return List.of(funcDef);
        }).withParentsTakeContext(),
        (TokenFinder<FunctionDefinition>)block[0].consume(),
        (TokenFinder<FunctionDefinition>)TokenFinder.keyword(KeywordTokenType.END).throwing("expected statement or \"end\" in function").consume()
    );

    funcdec[0] = TokenFinder.ordered(
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.keyword(KeywordTokenType.FUNCTION).map((tokenFinder, keywords) -> {
          tokenFinder.context.put("method", false);
          return List.of(keywords.getFirst());
        }).withParentsTakeContext(),
        (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ordered(
            (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.ordered(
                TokenFinder.ofClass(Token.Name.class).map((tokenFinder, names) -> List.of(names.getFirst().text)),
                TokenFinder.zeroOrMore(TokenFinder.ordered(
                    (TokenFinder<String>)TokenFinder.ofClass(Token.PropertyDot.class).consume(),
                    TokenFinder.ofClass(Token.Name.class).map((tokenFinder, names) -> List.of(names.getFirst().text)).throwing("expected property key after dot")
                ))
            ).map((tokenFinder, strings) -> {
              VariableOrPropertyAccess expression = tokenFinder.block.findVariableOrGlobal(strings.removeFirst());
              while (!strings.isEmpty()) {
                expression = new PropertyAccess(expression, new Literal.StringLiteral(strings.removeFirst()));
              }
              return List.of(expression);
            }),
            (TokenFinder<Object>)(TokenFinder<?>)TokenFinder.optional(TokenFinder.ordered(
                (TokenFinder<String>)TokenFinder.ofClass(Token.PropertyColon.class).consume(),
                TokenFinder.ofClass(Token.Name.class).map((tokenFinder, names) -> List.of(names.getFirst().text)).throwing("expected property key after colon")
            ))
        ).map((tokenFinder, objects) -> {
          if (objects.size() == 2) {
            tokenFinder.context.put("method", true);
            return List.of(new PropertyAccess((VariableOrPropertyAccess)objects.getFirst(), new Literal.StringLiteral((String)objects.getLast())));
          } else if (objects.size() == 1) {
            tokenFinder.context.put("method", false);
            return List.of((VariableOrPropertyAccess)objects.getFirst());
          } else throw new IllegalStateException();
        }).withParentsTakeContext(),
        (TokenFinder<Object>)(TokenFinder<?>)funcbody[0].throwing("expected function body after \"function <name>\"")
    ).map((tokenFinder, objects) -> {
      Token.Keyword keyword = (Token.Keyword)objects.getFirst();
      VariableOrPropertyAccess path = (VariableOrPropertyAccess)objects.get(1);
      FunctionDefinition funcDef = (FunctionDefinition)objects.getLast();
      return List.of((Assignment)new Assignment(new VariableOrPropertyAccess[]{path}, new FunctionDefinition[]{funcDef}).at(keyword));
    });

  }
}
