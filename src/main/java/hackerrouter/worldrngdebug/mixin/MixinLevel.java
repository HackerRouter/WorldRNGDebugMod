package hackerrouter.worldrngdebug.mixin;

import hackerrouter.worldrngdebug.state.RngState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {
    @Inject(method = "getBlockRandomPos", at = @At("HEAD"), cancellable = true)
    private void onGetBlockRandomPos(int xo, int yo, int zo, int yMask,
                                     CallbackInfoReturnable<BlockPos> cir) {
        if (RngState.isRandValueFrozen((Level)(Object)this)) {
            int val = ((LevelRandValueAccessor)(Object)this).getRandValue() >> 2;
            cir.setReturnValue(new BlockPos(
                xo + (val & 15),
                yo + (val >> 16 & yMask),
                zo + (val >> 8 & 15)
            ));
        }
    }
}
