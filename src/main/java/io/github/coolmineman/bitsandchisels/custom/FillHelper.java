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
    // TODO: Maximum chunks
    public static void fillBlocks(int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket) {

        BlockPos pos = MinecraftClient.getInstance().player.getBlockPos();

        // Find min/max corners relative to player position, in relative bits
        int minX = 16 * (Math.min(x1, x2) - pos.getX());
        int minY = 16 * (Math.min(y1, y2) - pos.getY());
        int minZ = 16 * (Math.min(z1, z2) - pos.getZ());
        int maxX = 16 * (Math.max(x1, x2) - pos.getX()) + 15;
        int maxY = 16 * (Math.max(y1, y2) - pos.getY()) + 15;
        int maxZ = 16 * (Math.max(z1, z2) - pos.getZ()) + 15;

        // Fill area in bits
        fillBits(pos, minX, minY, minZ, maxX, maxY, maxZ, mainhand, breakPacket);
    }

    // Fills an area specified by bits offset corners (x1, y1, z1) and (x2, y2, z2) and an origin pos (must be sorted)
    public static void fillBits(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket) {
        // Split into regions
        ArrayList<int[]> regions = splitRegion(x1, y1, z1, x2, y2, z2);

        // Only one region; process synchronously
        if (regions.size() == 1) {
            int[] r = regions.get(0);
            System.out.printf("[sync] Sending region (%s, %s, %s) -> (%s, %s, %s)...\n", r[0], r[1], r[2], r[3], r[4], r[5]);
            sendPacket(pos, r[0], r[1], r[2], r[3], r[4], r[5], mainhand, breakPacket);
        }

        // Multiple regions; process in a thread
        else {
            // Reverse array if we're breaking to break top down
            if (breakPacket) Collections.reverse(regions);

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            new Thread(() -> {
                for (int[] r : regions) {
                    System.out.printf("[async thread] Sending region (%s, %s, %s) -> (%s, %s, %s)...\n", r[0], r[1], r[2], r[3], r[4], r[5]);

                    // Recenter to player if unlocked mode is enabled (player may have moved)
                    if (Config.unlockedMode) {
                        BlockPos playerPos = player.getBlockPos();
                        r[0] += 16 * (pos.getX() - playerPos.getX());
                        r[1] += 16 * (pos.getY() - playerPos.getY());
                        r[2] += 16 * (pos.getZ() - playerPos.getZ());
                        r[3] += 16 * (pos.getX() - playerPos.getX());
                        r[4] += 16 * (pos.getY() - playerPos.getY());
                        r[5] += 16 * (pos.getZ() - playerPos.getZ());
                        sendPacket(playerPos, r[0], r[1], r[2], r[3], r[4], r[5], mainhand, breakPacket);
                    }
                    // Otherwise don't recenter
                    else sendPacket(pos, r[0], r[1], r[2], r[3], r[4], r[5], mainhand, breakPacket);
                    try {
                        Thread.sleep(Config.packetSendInterval);
                    }
                    catch (InterruptedException ignored) {}
                }
            }).start();
        }
    }

    // Sends a single packet to break or fill an area of bits from (x1, y1, z1) to (x2, y2, z2) ([xyz]1 must be < [xyz]2 for each axis)
    public static void sendPacket(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket) {
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
        System.out.printf("Splitting region (%s, %s, %s) -> (%s, %s, %s)...\n", x1, y1, z1, x2, y2, z2);
        ArrayList<int[]> regions = new ArrayList<>();
        // Volume fails server side sum of squares check
        if ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1) > 1000000) {
            System.out.printf("Failed server side check! Splitting along longest axis... ");
            // Split in half along longest access and recurse
            if (x2 - x1 > y2 - y1 && x2 - x1 > z2 - z1) {
                System.out.println("x axis");
                regions.addAll(splitRegion(x1, y1, z1, x1 + (x2 - x1) / 2, y2, z2));
                regions.addAll(splitRegion(x1 + (x2 - x1) / 2 + 1, y1, z1, x2, y2, z2));
            }
            else if (z2 - z1 > y2 - y1 && z2 - z1 > x2 - x1) {
                System.out.println("y axis");
                regions.addAll(splitRegion(x1, y1, z1, x2, y2, z1 + (z2 - z1) / 2));
                regions.addAll(splitRegion(x1, y1, z1 + (z2 - z1) / 2 + 1, x2, y2, z2));
            }
            else {
                System.out.println("z axis");
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
            System.out.printf("Failed volume check! Splitting into %s regions of size %s...\n", regionCount, regionSize);
            while (regionY <= y2) {
                System.out.printf("Generated region: (%s, %s, %s) -> (%s, %s, %s)\n", x1, regionY, z1, x2, Math.min(y2, regionY + regionSize - 1), z2);
                regions.add(new int[]{x1, regionY, z1, x2, Math.min(y2, regionY + regionSize - 1), z2});
                regionY += regionSize;
            }
        }
        else {
            System.out.println("Region is small enough");
            regions.add(new int[]{x1, y1, z1, x2, y2, z2});
        }

        return regions;
    }
}
