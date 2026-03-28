package io.github.coolmineman.bitsandchisels.mixin;

import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import io.github.coolmineman.bitsandchisels.custom.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class ToggleFullBlockMixin {
    @Inject(method = "doAttack()Z", at = @At("TAIL"))
    void doAttack(CallbackInfoReturnable<Boolean> c) {
        MinecraftClient thiz = (MinecraftClient) (Object) this;
        if (thiz.crosshairTarget.getType() == HitResult.Type.MISS) {
            ItemStack stack = thiz.player.getMainHandStack();
            Item item = stack.getItem();
            // Toggle full blocks only mode on swing at air
            if (item == BitsAndChisels.SMART_CHISEL || item == BitsAndChisels.BIT_ITEM) {
                Config.fullBlocksOnly = !Config.fullBlocksOnly;
            }
        }
    }
}
