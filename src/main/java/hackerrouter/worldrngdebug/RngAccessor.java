package hackerrouter.worldrngdebug;

import net.minecraft.world.level.levelgen.LegacyRandomSource;

public class RngAccessor {
    private static final long MASK = (1L << 48) - 1;
    private static final long MULTIPLIER = 0x5DEECE66DL;

    public static long getSeed(LegacyRandomSource random) {
        return random.seed.get() ^ MULTIPLIER;
    }

    public static void setSeed(LegacyRandomSource random, long seed) {
        random.seed.set((seed ^ MULTIPLIER) & MASK);
    }

    public static long getInternalSeed(LegacyRandomSource random) {
        return random.seed.get();
    }

    public static void advance(LegacyRandomSource random, long count) {
        long internal = random.seed.get();
        for (long i = 0; i < count; i++) {
            internal = (internal * MULTIPLIER + 11L) & MASK;
        }
        random.seed.set(internal);
    }
}
