package hackerrouter.worldrngdebug.mixin;

import hackerrouter.worldrngdebug.state.RngState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LegacyRandomSource.class)
public abstract class MixinLegacyRandomSource {
    @Inject(method = "next", at = @At("HEAD"), cancellable = true)
    private void onNext(int bits, CallbackInfoReturnable<Integer> cir) {
        LegacyRandomSource self = (LegacyRandomSource)(Object)this;
        if (RngState.isFrozen(self)) {
            long current = self.seed.get();
            cir.setReturnValue((int)(current >>> (48 - bits)));
        }
    }
}
