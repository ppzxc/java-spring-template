package io.github.ppzxc.boilerplate.identity.domain.model;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/** RFC 9562 §6.2 Method 1 — monotonic counter 기반 UUIDv7 생성기. */
public final class UUIDv7 {

  private UUIDv7() {}

  private record State(long timestamp, int counter) {}

  private static final AtomicReference<State> STATE = new AtomicReference<>(new State(0L, 0));

  public static UUID generate() {
    while (true) {
      State prev = Objects.requireNonNull(STATE.get());
      long ts = System.currentTimeMillis();
      long newTs = prev.timestamp();
      int newCnt;
      if (ts > prev.timestamp()) {
        newTs = ts;
        newCnt = ThreadLocalRandom.current().nextInt(0x1000);
      } else {
        newCnt = prev.counter() + 1;
      }
      if (newCnt >= 0x1000) {
        while (System.currentTimeMillis() == prev.timestamp()) {
          Thread.onSpinWait();
        }
        continue;
      }
      var next = new State(newTs, newCnt);
      if (STATE.compareAndSet(prev, next)) {
        long msb =
            ((next.timestamp() & 0x0000_FFFF_FFFF_FFFFL) << 16) | (0x7L << 12) | next.counter();
        long lsb =
            (0b10L << 62) | (ThreadLocalRandom.current().nextLong() & 0x3FFF_FFFF_FFFF_FFFFL);
        return new UUID(msb, lsb);
      }
    }
  }
}
