package underscore.andthereitgoes.shadepile.transpiler.lua.transpile;

public class NewlineCountingStringBuilder {
  private final StringBuilder stringBuilder;
  private int line = 1;

  public NewlineCountingStringBuilder() {
    this.stringBuilder = new StringBuilder();
  }
  public NewlineCountingStringBuilder(CharSequence sequence) {
    this.stringBuilder = new StringBuilder(sequence);
    this.line = (int)sequence.chars().filter(i -> i == '\n').count() + 1;
  }

  public void append(String s) {
    this.stringBuilder.append(s);
    this.line += (int)s.chars().filter(i -> i == '\n').count();
  }
  public void append(StringBuffer s) {
    this.stringBuilder.append(s);
    this.line += (int)s.chars().filter(i -> i == '\n').count();
  }
  public void append(char c) {
    this.stringBuilder.append(c);
    if (c == '\n') this.line++;
  }
  public void append(CharSequence s) {
    this.stringBuilder.append(s);
    this.line += (int)s.chars().filter(i -> i == '\n').count();
  }
  public void repeatNewline(int count) {
    this.stringBuilder.repeat('\n', count);
    this.line += count;
  }

  public int getLine() {
    return line;
  }
}
