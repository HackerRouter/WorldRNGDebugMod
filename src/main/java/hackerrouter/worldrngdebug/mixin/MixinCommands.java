package hackerrouter.worldrngdebug.mixin;

import com.mojang.brigadier.CommandDispatcher;
import hackerrouter.worldrngdebug.command.WrngCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class MixinCommands {
    @Shadow private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>*", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        WrngCommand.register(this.dispatcher);
    }
}