package hackerrouter.worldrngdebug.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface LevelRandValueAccessor {
    @Accessor("randValue") int getRandValue();
    @Accessor("randValue") void setRandValue(int value);
    @Accessor("random") RandomSource getRandom();
}
