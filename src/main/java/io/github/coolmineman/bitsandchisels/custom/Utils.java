package io.github.coolmineman.bitsandchisels.custom;

import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class Utils {
    // Calculates the volume of an operation
    public static double volume(Operation op) {
        return (double) (op.x2() - op.x1() + 1) * (op.y2() - op.y1() + 1) * (op.z2() - op.z1() + 1) / 4096;
    }

    // Checks whether the item held matches the operation type
    public static boolean itemIncorrect(OperationType type, Hand hand) {
        if (MinecraftClient.getInstance().player == null) return true;
        Item item;
        if (hand == Hand.MAIN_HAND)
            item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        else item = MinecraftClient.getInstance().player.getOffHandStack().getItem();
        return (type == OperationType.BREAK && item != BitsAndChisels.SMART_CHISEL) ||
               (type == OperationType.PLACE && item != BitsAndChisels.BIT_ITEM);
    }

    // Calculate the volume between two BlockPos
    public static int volume(BlockPos pos1, BlockPos pos2) {
        return (Math.abs(pos2.getX() - pos1.getX()) + 1) *
                (Math.abs(pos2.getY() - pos1.getY()) + 1) *
                (Math.abs(pos2.getZ() - pos1.getZ()) + 1);
    }
}
