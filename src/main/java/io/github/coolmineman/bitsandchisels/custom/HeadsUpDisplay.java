package io.github.coolmineman.bitsandchisels.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class HeadsUpDisplay {

    // XxYxZ: N blocks, M fills, HH:MM:SS; 47 in queue, HH:MM:SS
    public static void displayFullHUD(int x1, int y1, int z1, int x2, int y2, int z2) {
        DecimalFormat df = new DecimalFormat("0");
        double volume = (double) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1) / 4096;

        // Calculate time
        ArrayList<int[]> regions = FillHelper.splitRegion(x1, y1, z1, x2, y2, z2);
        int time = 0;
        for (int[] r : regions) {
            double subvolume = (double) (r[3] - r[0] + 1) * (r[4] - r[1] + 1) * (r[5] - r[2] + 1) / 4096;
            time += (int) ((subvolume / Config.maxPacketVolume) * Config.maxPacketSendInterval);
        }
        int h = time / 1000 / 60 / 60;
        int m = (time / 1000 / 60) % 60;
        int s = (time / 1000) % 60;

        // Set colors
        Formatting numColor = volume <= Config.maxVolumeManual ? Formatting.GREEN : Formatting.RED;
        Formatting otherColor = volume <= Config.maxVolumeManual ? Formatting.YELLOW : Formatting.RED;
        df.setMaximumFractionDigits(4);
        // AxBxC, N blocks, M fills, [HH:]MM:SS; K in queue, [HH:]MM:SS
        Text msg = Text.empty()
                .append(Text.literal(df.format((double) (x2 - x1 + 1) / 16)).formatted(numColor))
                .append(Text.literal("x").formatted(otherColor))
                .append(Text.literal(df.format((double) (y2 - y1 + 1) / 16)).formatted(numColor))
                .append(Text.literal("x").formatted(otherColor))
                .append(Text.literal(df.format((double) (z2 - z1 + 1) / 16)).formatted(numColor))
                .append(Text.literal(", ").formatted(otherColor))
                .append(Text.literal(df.format(volume)).formatted(numColor))
                .append(Text.literal(" blocks, ").formatted(otherColor))
                .append(Text.literal(df.format(FillHelper.splitRegion(x1, y1, z1, x2, y2, z2).size())).formatted(numColor))
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
        int h2 = FillHelper.estDelay / 1000 / 60 / 60;
        int m2 = (FillHelper.estDelay / 1000 / 60) % 60;
        int s2 = (FillHelper.estDelay / 1000) % 60;

        Formatting numColor = FillHelper.holdingCorrectItem ? Formatting.GREEN : Formatting.RED;
        Formatting otherColor = FillHelper.holdingCorrectItem ? Formatting.YELLOW : Formatting.RED;
        Text msg = Text.empty()
            .append(Text.literal(Integer.toString(FillHelper.fillQueue.size())).formatted(numColor))
            .append(Text.literal(" in queue, ").formatted(otherColor))
            .append(Text.literal(h2 > 0 ? String.format("%02d", h2) : "").formatted(numColor))
            .append(Text.literal(h2 > 0 ? ":" : "").formatted(otherColor))
            .append(Text.literal(String.format("%02d", m2)).formatted(numColor))
            .append(Text.literal(":").formatted(otherColor))
            .append(Text.literal(String.format("%02d", s2)).formatted(numColor));
        if (!FillHelper.holdingCorrectItem) {
            msg = msg.copy().append(Text.literal(String.format(", Not holding %s!", FillHelper.holdingCorrect(true) ? "bits" : "Smart Chisel")).formatted(Formatting.RED));
        }
        return msg;
    }
}
