package underscore.andthereitgoes.shadepile.transpiler.lua.transpile;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;


public class StringAnchorMapper {
  private int numMappings;
  private int[] mappings;

  public StringAnchorMapper() {
    this.mappings = new int[0];
    this.numMappings = 0;
  }

  public void addMapping(int inputLine, int inputColumn, long outputPosition) {
    int i = this.numMappings * 3;
    this.mappings = Arrays.copyOf(this.mappings, i + 3);
    if (outputPosition > (long)Integer.MAX_VALUE) throw new StringIndexOutOfBoundsException("input code is too long (about 2 GB)");
    this.mappings[i] = inputLine;
    this.mappings[i+1] = inputColumn;
    this.mappings[i+2] = (int)outputPosition;
    this.numMappings++;
  }

  @Contract(pure = true)
  public int @Nullable [] findInputLineColumn(long outputPosition) {
    return findInputLineColumn((int)outputPosition);
  }

  @Contract(pure = true)
  public int @Nullable [] findInputLineColumn(int outputPosition) {
    for (int n = 0; n < this.numMappings; n++) {
      int i = n * 3;
      if (this.mappings[i + 2] > outputPosition) break;
      if (n == this.numMappings - 1 || this.mappings[i + 5] > outputPosition) {
        return new int[]{this.mappings[i], this.mappings[i+1]};
      }
    }
    return null;
  }
}
