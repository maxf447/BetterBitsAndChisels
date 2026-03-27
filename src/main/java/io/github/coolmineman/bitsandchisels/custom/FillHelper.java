package io.github.coolmineman.bitsandchisels.custom;

import io.netty.buffer.Unpooled;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;


public class FillHelper {

    // Fills an area specified by corners in blocks (x1, y1, z1) and (x2, y2, z2)
    // TODO: Maximum chunks
    public static void fillBlocks(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2, boolean mainhand, boolean breakPacket) {

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
        sendPacket(pos, x1, y1, z1, x2, y2, z2, mainhand, breakPacket);
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

}
