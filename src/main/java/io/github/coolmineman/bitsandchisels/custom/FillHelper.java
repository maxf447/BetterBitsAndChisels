package io.github.coolmineman.bitsandchisels.custom;

import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.Collections;


public class FillHelper {

    // Fills an area specified by corners in blocks (x1, y1, z1) and (x2, y2, z2)
    public static void fillBlocks(int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket) {

        // Find min/max corners in bits
        int minX = 16 * Math.min(x1, x2);
        int minY = 16 * Math.min(y1, y2);
        int minZ = 16 * Math.min(z1, z2);
        int maxX = 16 * Math.max(x1, x2) + 15;
        int maxY = 16 * Math.max(y1, y2)+ 15;
        int maxZ = 16 * Math.max(z1, z2) + 15;

        // Fill area in bits
        fillBits(BlockPos.ORIGIN, minX, minY, minZ, maxX, maxY, maxZ, mainhand, breakPacket, true);
    }

    // Fills an area specified by bits offset corners (x1, y1, z1) and (x2, y2, z2) and an origin pos (must be sorted)
    public static void fillBits(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket, boolean recenter) {
        // Split into regions
        ArrayList<int[]> regions = splitRegion(x1, y1, z1, x2, y2, z2);

        // Only one region; process synchronously
        if (regions.size() == 1) {
            int[] r = regions.get(0);
            sendPacket(pos, r[0], r[1], r[2], r[3], r[4], r[5], mainhand, breakPacket, recenter);
        }

        // Multiple regions; process in a thread
        else {
            // Reverse array if we're breaking to break top down
            if (breakPacket) Collections.reverse(regions);

            new Thread(() -> {
                for (int[] r : regions) {
                    sendPacket(pos, r[0], r[1], r[2], r[3], r[4], r[5], mainhand, breakPacket, recenter);
                    try {
                        Thread.sleep(Config.packetSendInterval);
                    }
                    catch (InterruptedException ignored) {}
                }
            }).start();
        }
    }

    public static void fillSphere(BlockPos center, double r, boolean mainhand, boolean breakPacket) {
        // Convert radius to bits
        r *= 16;
        // Fill central cuboid
        int c = (int) (r / Math.sqrt(3));
        fillBits(center, -c, -c, -c, c, c, c, mainhand, breakPacket, true);
    }

    public static void fillAboveFace() {}

    // Sends a single packet to break or fill an area of bits from (x1, y1, z1) to (x2, y2, z2) ([xyz]1 must be < [xyz]2 for each axis)
    public static void sendPacket(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket, boolean recenter) {
        // Recenter to player if unlocked mode is enabled
        if (recenter) {
            BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
            x1 += 16 * (pos.getX() - playerPos.getX());
            y1 += 16 * (pos.getY() - playerPos.getY());
            z1 += 16 * (pos.getZ() - playerPos.getZ());
            x2 += 16 * (pos.getX() - playerPos.getX());
            y2 += 16 * (pos.getY() - playerPos.getY());
            z2 += 16 * (pos.getZ() - playerPos.getZ());
            pos = playerPos;
        }

        // Create and send packet
        PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer());
        packet.writeBlockPos(pos);
        packet.writeInt(x1);
        packet.writeInt(y1);
        packet.writeInt(z1);
        packet.writeInt(x2);
        packet.writeInt(y2);
        packet.writeInt(z2);
        if (!breakPacket) packet.writeBoolean(mainhand);
        ClientPlayNetworking.send(new Identifier("bitsandchisels", breakPacket ? "smart_chisel_packet" : "bit_packet"), packet);
    }

    // Takes a single volume (in bits) and splits it into an array of smaller volumes that pass serverside checks and volume check
    public static ArrayList<int[]> splitRegion(int x1, int y1, int z1, int x2, int y2, int z2) {
        ArrayList<int[]> regions = new ArrayList<>();
        // Volume fails server side sum of squares check
        if ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1) > 1000000) {
            // Split in half along longest access and recurse
            if (x2 - x1 > y2 - y1 && x2 - x1 > z2 - z1) {
                regions.addAll(splitRegion(x1, y1, z1, x1 + (x2 - x1) / 2, y2, z2));
                regions.addAll(splitRegion(x1 + (x2 - x1) / 2 + 1, y1, z1, x2, y2, z2));
            }
            else if (z2 - z1 > y2 - y1 && z2 - z1 > x2 - x1) {
                regions.addAll(splitRegion(x1, y1, z1, x2, y2, z1 + (z2 - z1) / 2));
                regions.addAll(splitRegion(x1, y1, z1 + (z2 - z1) / 2 + 1, x2, y2, z2));
            }
            else {
                regions.addAll(splitRegion(x1, y1, z1, x2, y1 + (y2 - y1) / 2, z2));
                regions.addAll(splitRegion(x1, y1 + (y2 - y1) / 2 + 1, z1, x2, y2, z2));
            }
        }

        // Volume fails client side volume check
        double volume = (double) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1) / 4096;
        if (volume > Config.maxPacketVolume) {
            // Split along y-axis into minimum number of regions to pass volume check
            int regionCount = (int) Math.ceil(volume / Config.maxPacketVolume);

            int regionSize = Math.max(1, (y2 - y1 + 1) / regionCount);
            int regionY = y1;

            // Add to regions list
            while (regionY <= y2) {
                regions.add(new int[]{x1, regionY, z1, x2, Math.min(y2, regionY + regionSize - 1), z2});
                regionY += regionSize;
            }
        }
        else {
            regions.add(new int[]{x1, y1, z1, x2, y2, z2});
        }

        return regions;
    }
}
