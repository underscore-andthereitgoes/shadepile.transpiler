package underscore.andthereitgoes.shadepile.transpiler.lua.parse;

import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import underscore.andthereitgoes.shadepile.transpiler.lua.KeywordTokenType;
import underscore.andthereitgoes.shadepile.transpiler.lua.tokenize.Token;
import underscore.andthereitgoes.shadepile.transpiler.lua.transpile.ast.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class TokenFinder <T> implements Supplier<TokenFinder<T>> {

  public final HashMap<String,Object> context = new HashMap<>();
  private final BiFunction<@NotNull TokenFinder<?>, @NotNull Parser, @Nullable List<T>> testFunc;
  public Block block = null;
  private boolean parentsTakeContext = false;

  /// Set the `Block` used for child token finders.
  @Contract(value = "_ -> this", pure = true)
  public TokenFinder<T> withBlock(Block block) {
    this.block = block;
    return this;
  }

  /// Tells parent token finders to copy this token finder's block and update from its context when it validates. Chain this AFTER `map` and `throwing`.
  @Contract(value = "-> this", pure = true)
  public TokenFinder<T> withParentsTakeContext() {
    this.parentsTakeContext = true;
    return this;
  }

  /// Sets whether parent token finders should copy this token finder's block and update from its context when it validates. Chain this AFTER `map` and `throwing`.
  @Contract(value = "_ -> this", pure = true)
  public TokenFinder<T> withParentsTakeContext(boolean parentsTakeContext) {
    this.parentsTakeContext = parentsTakeContext;
    return this;
  }

  /// Adds context to this token finder. Context properties overwrite child token finders. Note that context is never reset.
  @Contract(value = "_,_ -> this")
  public TokenFinder<T> withContext(String key, Object value) {
    this.context.put(key, value);
    return this;
  }

  /// Clears this token finder's context.
  @Contract(value = "-> this")
  public TokenFinder<T> withoutContext() {
    this.context.clear();
    return this;
  }

  protected void mergeContext(Map<String,Object> from) {
    this.context.putAll(from);
  }

  private TokenFinder(BiFunction<@NotNull TokenFinder<?>, @NotNull Parser, @Nullable List<T>> condition) {
    this.testFunc = condition;
  }

  /// Test this token finder against a parser, returning the test results if it is valid, or `null` if it isn't.
  public @Nullable List<T> test(@NotNull Parser parser) {
    var ptr0 = parser.tell();
    var results = this.testFunc.apply(this, parser);
    if (results == null) parser.seek(ptr0);
    return results;
  }

  /// Creates a new token finder with the same test but a mapping function applied afterwards regardless of validity. If the mapping function returns `null`, the finder invalidates.
  @Contract(value = "_ -> new", pure = true)
  public <R> TokenFinder<R> mapAlways(BiFunction<TokenFinder<?>, @Nullable List<T>, @Nullable List<R>> mapper) {
    return new TokenFinder<>((finder, parser) -> mapper.apply(finder, this.test(parser)));
  }

  /// Creates a new token finder with the same test but a mapping function applied afterwards regardless of validity. If the mapping function returns `null`, the finder invalidates.
  @Contract(value = "_ -> new", pure = true)
  public <R> TokenFinder<R> mapAlwaysWithParser(TriFunction<TokenFinder<?>, @NotNull Parser, @Nullable List<T>, @Nullable List<R>> mapper) {
    return new TokenFinder<>((finder, parser) -> mapper.apply(finder, parser, this.test(parser)));
  }

  /// Creates a new token finder with the same test but a mapping function applied afterwards if it's valid. If the mapping function returns `null`, the finder invalidates.
  @Contract(value = "_ -> new", pure = true)
  public <R> TokenFinder<R> map(BiFunction<TokenFinder<?>, @NotNull List<T>, @Nullable List<R>> mapper) {
    return new TokenFinder<>((finder, parser) -> {
      final @Nullable List<T> r = this.test(parser);
      if (r != null) return mapper.apply(finder, r);
      return null;
    });
  }

  /// Creates a new token finder with the same test but throwing an error if invalid.
  @Contract(value = "_ -> new", pure = true)
  public TokenFinder<T> throwing(String errorMessage) {
    return this.mapAlwaysWithParser((tf, p, t) -> {
      if (t == null) {
        LuaParseError e = new LuaParseError(errorMessage);
        e.at = p.look(0);
        throw e;
      }
      return t;
    });
  }

  /// Creates a new token finder with the same test but an empty list for results.
  @Contract(value = "-> new", pure = true)
  public TokenFinder<?> consume() {
    return this.map((tf, t) -> List.of());
  }

  /// Creates a wrapper around this token finder that returns the same result in a one-element list.
  public TokenFinder<List<T>> group() {
    return this.map((tf, t) -> List.of(t));
  }

  /// Creates a token finder by the test function. This function should take the token finder and a parser and return a list of results if it's valid, or `null` if it's invalid.
  @Contract(value = "_ -> new", pure = true)
  public static <T> TokenFinder<T> create(BiFunction<@NotNull TokenFinder<?>, @NotNull Parser, @Nullable List<T>> condition) {
    return new TokenFinder<>(condition);
  }

  /// Creates a token finder that is always valid and results in an empty list.
  @Contract(value = "-> new", pure = true)
  public static TokenFinder<?> emptyValid() {
    return new TokenFinder<>((tf, t) -> List.of());
  }

  /// Creates a token finder that is always valid and results in the given *supplied* value, computed per validation.
  @Contract(value = "_ -> new", pure = true)
  public static <C> TokenFinder<C> constantValid(Supplier<List<C>> x) {
    return new TokenFinder<>((tf, t) -> x.get());
  }

  /// Creates a token finder that is always valid and results in the given value.
  @Contract(value = "_ -> new", pure = true)
  public static <C> TokenFinder<C> constantValid(List<C> x) {
    return new TokenFinder<>((tf, t) -> x);
  }

  /// Creates a token finder that is always invalid.
  @Contract(value = "-> new", pure = true)
  public static TokenFinder<?> invalid() {
    return new TokenFinder<>((tf, p) -> null);
  }

  @Contract("-> this")
  @Override
  public TokenFinder<T> get() {
    return this;
  }

  @FunctionalInterface
  public interface TokenConditionFunction {
    boolean check(@NotNull TokenFinder<?> tokenFinder, @NotNull Token token, @NotNull Parser parser);
  }

  /// Creates a token finder that takes the next token and checks it against a condition function, returning the token if it's valid or `null` if it isn't.
  @Contract(value = "_ -> new", pure = true)
  public static TokenFinder<Token> tokenCondition(TokenConditionFunction condition) {
    return new TokenFinder<>((tf, parser) -> condition.check(tf, parser.peek(), parser) ? List.of(parser.take()) : null);
  }

  /// Creates a token finder that checks each condition in order, concatenating their results into an array, until they're all validated, or exits invalidly when any condition isn't valid. Each supplier is evaluated exactly once, upon the first validation request.
  @Contract(value = "_ -> new", pure = true)
  public static <T> TokenFinder<T> ordered(Supplier<@NotNull TokenFinder<T>>... order) {
    @SuppressWarnings("unchecked")
    TokenFinder<T>[] cachedFinders = new TokenFinder[order.length];
    return new TokenFinder<>(((tf, parser) -> {

      if (cachedFinders.length > 0 && cachedFinders[0] == null) for (int i = 0; i < order.length; i++) cachedFinders[i] = order[i].get();

      List<T> out = new ArrayList<>();
      int ptr0 = parser.tell();

      for (var finder: cachedFinders) {
        if (tf.block != null) finder.block = tf.block;
        finder.mergeContext(tf.context);
        var results = finder.test(parser);
        if (results == null) {
          parser.seek(ptr0);
          return null;
        }
        if (finder.parentsTakeContext) {
          tf.block = finder.block;
          tf.mergeContext(finder.context);
        }
        out.addAll(results);
      }

      return out;

    }));
  }

  /// Creates a wrapper around a token finder that validates with an empty array instead of invalidating.
  @Contract(value = "_ -> new", pure = true)
  public static <T> TokenFinder<T> optional(@NotNull TokenFinder<T> finder) {
    return finder.mapAlways((tokenFinder, ts) -> ts == null ? List.of() : ts);
  }

  /// Creates a wrapper around a token finder that repeatedly tests the same condition in order, concatenating the results into an array. Always validates. Takes a supplier, and evaluates it once when validation is first requested.
  @Contract(value = "_ -> new", pure = true)
  public static <T> TokenFinder<T> zeroOrMore(Supplier<@NotNull TokenFinder<T>> finderSup) {
    @SuppressWarnings("unchecked")
    final TokenFinder<T>[] cached = new TokenFinder[1];
    return new TokenFinder<>(((tf, parser) -> {
      if (cached[0] == null) cached[0] = finderSup.get();
      TokenFinder<T> finder = cached[0];
      List<T> out = new ArrayList<>();
      while (true) {
        if (tf.block != null) finder.block = tf.block;
        finder.mergeContext(tf.context);
        int ptr0 = parser.tell();
        List<T> results = finder.test(parser);
        if (results == null) {
          parser.seek(ptr0);
          break;
        }
        if (finder.parentsTakeContext) {
          tf.block = finder.block;
          tf.mergeContext(finder.context);
        }
        out.addAll(results);
      }
      return out;
    }));
  }

  /// Creates a wrapper around a token finder that repeatedly tests the same condition in order, concatenating the results into an array. Invalidates if the test never passed. Takes a supplier, and evaluates it once when validation is first requested.
  @Contract(value = "_ -> new", pure = true)
  public static <T> TokenFinder<T> oneOrMore(Supplier<@NotNull TokenFinder<T>> finderSup) {
    @SuppressWarnings("unchecked")
    final TokenFinder<T>[] cached = new TokenFinder[1];
    return new TokenFinder<>(((tf, parser) -> {
      if (cached[0] == null) cached[0] = finderSup.get();
      TokenFinder<T> finder = cached[0];
      List<T> out = new ArrayList<>();
      boolean anyPass = false;
      while (true) {
        if (tf.block != null) finder.block = tf.block;
        finder.mergeContext(tf.context);
        int ptr0 = parser.tell();
        List<T> results = finder.test(parser);
        if (results == null) {
          parser.seek(ptr0);
          break;
        }
        if (finder.parentsTakeContext) {
          tf.block = finder.block;
          tf.mergeContext(finder.context);
        }
        out.addAll(results);
        anyPass = true;
      }
      if (!anyPass) return null;
      return out;
    }));
  }

  /// Creates a token finder that checks an array of token finders in order and returns the first valid one, rewinding before each check, or null if none of them are valid. Also resets `block` to its previous value between each check.
  @Contract(value = "_ -> new", pure = true)
  public static <T> TokenFinder<T> firstValid(Supplier<@NotNull TokenFinder<T>>... order) {
    @SuppressWarnings("unchecked")
    TokenFinder<T>[] cachedFinders = new TokenFinder[order.length];
    return new TokenFinder<>(((tf, parser) -> {

      if (cachedFinders.length > 0 && cachedFinders[0] == null) for (int i = 0; i < order.length; i++) cachedFinders[i] = order[i].get();

      int ptr0 = parser.tell();

      for (var finder: cachedFinders) {
        finder.block = tf.block;
        finder.mergeContext(tf.context);
        List<T> results = finder.test(parser);
        if (results != null) {
          if (finder.parentsTakeContext) {
            tf.block = finder.block;
            tf.mergeContext(finder.context);
          }
          return results;
        }
        parser.seek(ptr0);
      }

      return null;

    }));
  }

  /// Creates a token finder that validates if the next token is an instance of the given token class.
  @Contract(value = "_ -> new", pure = true)
  public static <T extends Token> TokenFinder<T> ofClass(Class<T> clazz) {
    //noinspection unchecked
    return (TokenFinder<T>)tokenCondition(((tokenFinder, token, parser) -> token.getClass() == clazz));
  }

  /// Creates a token finder that validates if the next token is an instance of the given token class and also conforms to a predicate.
  @Contract(value = "_,_ -> new", pure = true)
  public static <T extends Token> TokenFinder<T> ofClassAndAlso(Class<T> clazz, Predicate<T> andAlso) {
    //noinspection unchecked
    return (TokenFinder<T>)tokenCondition(((tokenFinder, token, parser) -> token.getClass() == clazz && andAlso.test((T)token)));
  }

  /// Creates a token finder that validates if the next token is a `Keyword` with the given `kw` value, resulting in that token.
  @Contract(value = "_ -> new", pure = true)
  public static TokenFinder<Token.Keyword> keyword(KeywordTokenType kw) {
    return ofClassAndAlso(Token.Keyword.class, (keyword -> keyword.type == kw));
  }
}
