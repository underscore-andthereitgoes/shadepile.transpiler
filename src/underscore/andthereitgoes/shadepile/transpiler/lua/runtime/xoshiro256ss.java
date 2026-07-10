package underscore.andthereitgoes.shadepile.transpiler.lua.runtime;

import java.util.Random;


public class xoshiro256ss {
  private long s0;
  private long s1;
  private long s2;
  private long s3;

  public xoshiro256ss() {
    var random = new Random();
    random.setSeed(System.currentTimeMillis());
    s0 = random.nextLong();
    s1 = random.nextLong();
    s2 = random.nextLong();
    s3 = random.nextLong();
  }

  synchronized public void seed(long s0, long s1) {
    this.s0 = s0;
    this.s1 = s1;
    this.s2 = 0;
    this.s3 = 0;
  }

  synchronized public long nextLong() {
    final long result = Long.rotateLeft(s1 * 5L, 7) * 9L;
    final long t = s1 << 17;
    s2 ^= s0;
    s3 ^= s1;
    s1 ^= s2;
    s0 ^= s3;
    s2 ^= t;
    s3 = Long.rotateLeft(s3, 45);
    return result;
  }

  // copied from Random.nextLong
  synchronized public long nextLong(long bound) {
    final long m = bound - 1;
    long r = nextLong();
    if ((bound & m) == 0L) {
      r &= m;
    } else {
      //noinspection StatementWithEmptyBody
      for (long u = r >>> 1;
           u + m - (r = u % bound) < 0L;
           u = nextLong() >>> 1)
        ;
    }
    return r;
  }

  synchronized public double nextDouble() {
    return (nextLong() >>> 11) * 0x1.0p-53;
  }
}
