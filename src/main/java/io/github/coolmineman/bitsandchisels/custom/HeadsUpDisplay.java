package io.github.coolmineman.bitsandchisels.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import java.text.DecimalFormat;

public class HeadsUpDisplay {

    public static void displayFullHUD(int x1, int y1, int z1, int x2, int y2, int z2) {

        // Create dummy operation
        Operation op = new Operation(BlockPos.ORIGIN, x1, y1, z1, x2, y2, z2, Hand.MAIN_HAND, OperationType.PLACE, false);
        DecimalFormat df = new DecimalFormat("0");
        double volume = Utils.volume(op);

        // Calculate time
        int time = OperationProcessor.getTime(op);
        int h = time / 1000 / 60 / 60;
        int m = (time / 1000 / 60) % 60;
        int s = (time / 1000) % 60;

        // Set colors
        Formatting numColor = volume <= Config.maxVolumeManual ? Formatting.GREEN : Formatting.RED;
        Formatting otherColor = volume <= Config.maxVolumeManual ? Formatting.YELLOW : Formatting.RED;
        df.setMaximumFractionDigits(4);

        // Construct message
        Text msg = Text.empty()
            .append(Text.literal(df.format((double) (x2 - x1 + 1) / 16)).formatted(numColor))
            .append(Text.literal("x").formatted(otherColor))
            .append(Text.literal(df.format((double) (y2 - y1 + 1) / 16)).formatted(numColor))
            .append(Text.literal("x").formatted(otherColor))
            .append(Text.literal(df.format((double) (z2 - z1 + 1) / 16)).formatted(numColor))
            .append(Text.literal(", ").formatted(otherColor))
            .append(Text.literal(df.format(volume)).formatted(numColor))
            .append(Text.literal(" blocks, ").formatted(otherColor))
            .append(Text.literal(df.format(OperationProcessor.getSplitCount(op))).formatted(numColor))
            .append(Text.literal(" fills, ").formatted(otherColor))
            .append(Text.literal(h > 0 ? String.format("%02d", h) : "").formatted(numColor))
            .append(Text.literal(h > 0 ? ":" : "").formatted(otherColor))
            .append(Text.literal(String.format("%02d", m)).formatted(numColor))
            .append(Text.literal(":").formatted(otherColor))
            .append(Text.literal(String.format("%02d", s)).formatted(numColor))
            .append(Text.literal(" | ").formatted(otherColor))
            .append(stackStatus());
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(msg, false);

    }
    public static Text stackStatus() {
        // Calculate stack times
        int h = PacketQueue.getQueueDelay() / 1000 / 60 / 60;
        int m = (PacketQueue.getQueueDelay() / 1000 / 60) % 60;
        int s = (PacketQueue.getQueueDelay() / 1000) % 60;

        // Set colors
        Formatting numColor = PacketQueue.getWaitingForItem() == null ? Formatting.GREEN : Formatting.RED;
        Formatting otherColor = PacketQueue.getWaitingForItem() == null ? Formatting.YELLOW : Formatting.RED;

        // Construct message
        Text msg = Text.empty()
            .append(Text.literal(Integer.toString(PacketQueue.getQueueSize())).formatted(numColor))
            .append(Text.literal(" in queue, ").formatted(otherColor))
            .append(Text.literal(h > 0 ? String.format("%02d", h) : "").formatted(numColor))
            .append(Text.literal(h > 0 ? ":" : "").formatted(otherColor))
            .append(Text.literal(String.format("%02d", m)).formatted(numColor))
            .append(Text.literal(":").formatted(otherColor))
            .append(Text.literal(String.format("%02d", s)).formatted(numColor));

        // Add warning if wrong item is being held
        if (PacketQueue.getWaitingForItem() != null) {
            String item = PacketQueue.getWaitingForItem() == OperationType.PLACE ? "bits" : "Smart Chisel";
            msg = msg.copy().append(Text.literal(String.format(", Not holding %s!", item)).formatted(Formatting.RED));
        }

        return msg;
    }
}
