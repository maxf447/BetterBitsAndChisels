package io.github.coolmineman.bitsandchisels.custom;

import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class FillHelper {

    // Queue of fill operations to send
    public static final BlockingQueue<int[]> fillQueue = new LinkedBlockingQueue<>();
    public static int estDelay = 0;
    public static boolean holdingCorrectItem = true;

    // Fills a structure specified by a 3D array of values extending along the positive axes from the origin
    public static void fillStructure(BlockPos pos, int x1, int y1, int z1, boolean[][][] arr, boolean mainhand, boolean breakPacket) {
        // Set initial resolution
        int resX = 1;
        int resY = 1;
        int resZ = 1;
        while (resX < arr.length) resX *= 2;
        while (resY < arr[0].length) resY *= 2;
        while (resZ < arr[0][0].length) resZ *= 2;

        // Iterate until we've exhausted all resolution scales
        while (resX >= 1 && resY >= 1 && resZ >= 1) {
            // Iterate at given resolution and fill any full cubes
            for (int x = 0; x < arr.length; x += resX) {
                for (int y = 0; y < arr[0].length; y += resY) {
                    for (int z = 0; z < arr[0][0].length; z += resZ) {
                        if (emptyIfFilled(arr, x, y, z, x + resX - 1, y + resY - 1, z + resZ - 1))
                            fillBits(pos, x + x1, y + y1, z + z1, x + x1 + resX - 1, y + y1 + resY - 1, z + z1 + resZ - 1, mainhand, breakPacket, true);
                    }
                }
            }
            // Halve the greatest resolution
            if (resY >= resX && resY >= resZ) resY /= 2;
            else if (resX >= resY && resX > resZ) resX /= 2;
            else resZ /= 2;
        }
    }

    // Check whether entire subarray (x1, y1, z1) -> (x2, y2, z2) is filled, and empty it if so
    private static boolean emptyIfFilled(boolean[][][] arr, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1; x <= Math.min(arr.length - 1, x2); x++)
            for (int y = y1; y <= Math.min(arr[0].length - 1, y2); y++)
                for (int z = z1; z <= Math.min(arr[0][0].length - 1, z2); z++)
                    if (!arr[x][y][z]) return false;
        // Empty if we did not exit previously
        for (int x = x1; x <= Math.min(arr.length - 1, x2); x++)
            for (int y = y1; y <= Math.min(arr[0].length - 1, y2); y++)
                for (int z = z1; z <= Math.min(arr[0][0].length - 1, z2); z++)
                    arr[x][y][z] = false;
        return true;
    }


    // Fills an area specified by corners in blocks (x1, y1, z1) and (x2, y2, z2)
    public static void fillBlocks(int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket) {

        // Find min/max corners in bits
        int minX = 16 * Math.min(x1, x2);
        int minY = 16 * Math.min(y1, y2);
        int minZ = 16 * Math.min(z1, z2);
        int maxX = 16 * Math.max(x1, x2) + 15;
        int maxY = 16 * Math.max(y1, y2) + 15;
        int maxZ = 16 * Math.max(z1, z2) + 15;

        // Fill area in bits
        fillBits(BlockPos.ORIGIN, minX, minY, minZ, maxX, maxY, maxZ, mainhand, breakPacket, true);
    }

    // Fills an area specified by bits offset corners (x1, y1, z1) and (x2, y2, z2) and an origin pos (must be sorted)
    public static void fillBits(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket, boolean recenter) {
        // Create operation from parameters
        Operation op = new Operation(
                pos, x1, y1, z1, x2, y2, z2,
                mainhand ? Hand.MAIN_HAND : Hand.OFF_HAND,
                breakPacket ? OperationType.BREAK : OperationType.PLACE,
                recenter
        );
        // Process operation
        OperationProcessor.processOperation(op);
    }

    public static boolean holdingCorrect(boolean breakPacket) {
        Item item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        return (breakPacket && item == BitsAndChisels.SMART_CHISEL) ||
                (!breakPacket && item == BitsAndChisels.BIT_ITEM);
    }

    // Fills a sphere centered at center, of radius r blocks
    public static void fillSphere(BlockPos center, double r0, double r1, boolean mainhand, boolean breakPacket) {
        // Allocate array for sphere
        int w = (int) (2 * r1 * 16);
        boolean[][][] arr = new boolean[w][w][w];
        // Fill array
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < w; y++) {
                for (int z = 0; z < w; z++) {
                    double xC = (double) (x - w / 2) / 16;
                    double yC = (double) (y - w / 2) / 16;
                    double zC = (double) (z - w / 2) / 16;
                    arr[x][y][z] = xC * xC + yC * yC + zC * zC >= r0 * r0 && xC * xC + yC * yC + zC * zC < r1 * r1;
                }
            }
        }
        // Fill sphere centered on center of block
        fillStructure(center, (int) -r1 * 16 + 8, (int) -r1 * 16 + 8, (int) -r1 * 16 + 8, arr, mainhand, breakPacket);
    }
}
