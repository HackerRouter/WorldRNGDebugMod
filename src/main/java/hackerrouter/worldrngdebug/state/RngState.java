package hackerrouter.worldrngdebug.state;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import java.util.WeakHashMap;
import java.util.Map;

public class RngState {
    private static final Map<LegacyRandomSource, Boolean> frozenRandom = new WeakHashMap<>();
    private static final Map<Level, Boolean> frozenRandValue = new WeakHashMap<>();

    public static boolean isFrozen(LegacyRandomSource r)      { return frozenRandom.getOrDefault(r, false); }
    public static boolean isRandValueFrozen(Level level)       { return frozenRandValue.getOrDefault(level, false); }
    public static void setFrozen(LegacyRandomSource r, boolean v)  { frozenRandom.put(r, v); }
    public static void setRandValueFrozen(Level level, boolean v)  { frozenRandValue.put(level, v); }
}
