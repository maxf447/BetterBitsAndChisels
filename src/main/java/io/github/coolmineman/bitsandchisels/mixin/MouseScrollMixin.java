package io.github.coolmineman.bitsandchisels.mixin;

import io.github.coolmineman.bitsandchisels.custom.ScrollCallback;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mouse.class, priority = 10)
public class MouseScrollMixin {
    @Inject(method = "onMouseScroll", cancellable = true,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 0))
    private void hookOnMouseScroll(long handle, double xOffset, double yOffset, CallbackInfo ci) {
        ActionResult result = ScrollCallback.EVENT.invoker().interact(yOffset);
        if (result == ActionResult.FAIL) ci.cancel();
    }
}
