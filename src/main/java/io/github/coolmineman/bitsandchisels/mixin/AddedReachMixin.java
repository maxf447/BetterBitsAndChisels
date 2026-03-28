package io.github.coolmineman.bitsandchisels.mixin;


import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import io.github.coolmineman.bitsandchisels.custom.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AddedReachMixin {

    @Inject(method = "getReachDistance", at = @At("RETURN"), cancellable = true)
    private void modifyReachDistance(CallbackInfoReturnable<Float> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        // Add reach when holding bit item or smart chisel
        if (mainHand.getItem() == BitsAndChisels.BIT_ITEM ||
            offHand.getItem() == BitsAndChisels.BIT_ITEM ||
            mainHand.getItem() == BitsAndChisels.SMART_CHISEL) {
            cir.setReturnValue(cir.getReturnValue() + (float) Config.addedReach);
        }
    }
}