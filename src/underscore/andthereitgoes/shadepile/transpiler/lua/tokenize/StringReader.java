package underscore.andthereitgoes.shadepile.transpiler.lua.tokenize;

import com.sun.source.tree.BreakTree;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


public class StringReader {

  private final String input;
  private final int inputLength;
  private int pointer = 0;

  public StringReader(String input) {
    this(input, false);
  }
  private StringReader(String input, boolean dontCorrectCRLF) {
    if (dontCorrectCRLF) {
      this.input = input;
    } else {
      this.input = input.replace("\r\n", "\n");
    }
    this.inputLength = this.input.length();
  }

  /// Computes and returns the current 1-based line number.
  @Contract(pure = true)
  public int getLine() {
    return (int)this.input.substring(0, this.pointer).chars().filter(ch -> ch == '\n').count() + 1;
  }
  /// Computes and returns the current 1-based column number.
  @Contract(pure = true)
  public int getColumn() {
    return this.pointer - this.input.substring(0, this.pointer).lastIndexOf('\n');
    // if no \n, index is -1, column is (pointer - -1) = (pointer + 1)
  }

  /// Returns `true` if there are characters left to read, and `false` otherwise.
  @Contract(pure = true)
  public boolean hasMore() {
    return this.pointer < this.inputLength;
  }

  /// Returns the current pointer position.
  @Contract(pure = true)
  public int tell() {
    return Math.min(this.pointer, this.inputLength);
  }
  /// Sets the pointer position.
  public void seek(int to) {
    this.pointer = Math.clamp(to, 0, this.inputLength);
  }

  /// Peek at the next character without moving the pointer. Throws an error if `hasMore()` is `false`. Also see `peek(n)`.
  @Contract(pure = true)
  public char peek1() {
    return this.input.charAt(this.pointer);
  }

  /// Peek at the next *n* characters without moving the pointer. May return less than *n* characters if less are available. For *n* = 1, use `peek1()`.
  @Contract(pure = true)
  public @NotNull String peek(int n) {
    return this.input.substring(this.pointer, Math.min(this.pointer + n, this.inputLength));
  }

  /// `peek1()` but with an offset, and returning a `String`, and returning `""` instead of erroring.
  @Contract(pure = true)
  public @NotNull String look(int offset) {
    int p = this.pointer + offset;
    return p >= 0 && p < this.inputLength ? this.input.substring(p, p+1) : "";
  }

  /// Read and return the next character, and advance the pointer. Throws an error if `hasMore()` is `false`. Also see `take(n)`.
  public char take1() {
    int p = this.pointer;
    this.pointer = Math.min(this.pointer + 1, this.inputLength);
    return this.input.charAt(p);
  }
  /// Read and return the next *n* characters and advance the pointer. May return less than *n* characters if less are available. For *n* = 1, use `take1()`.
  public @NotNull String take(int n) {
    return this.input.substring(this.pointer, this.pointer = Math.min(this.pointer + n, this.inputLength));
  }

  /// Return a clone of this reader.
  @Contract(pure = true)
  public @NotNull StringReader fork() {
    StringReader sr = new StringReader(this.input, true);
    sr.pointer = this.pointer;
    return sr;
  }
}
