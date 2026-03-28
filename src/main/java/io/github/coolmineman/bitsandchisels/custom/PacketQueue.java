package io.github.coolmineman.bitsandchisels.custom;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Global queue for packets that have yet to be sent
public class PacketQueue {
    private static final BlockingQueue<PendingPacket> packetQueue = new LinkedBlockingQueue<>();
    private static int totalQueueDelay = 0;

    public static void enqueuePacket(PendingPacket packet) {
        packetQueue.add(packet);
        totalQueueDelay += packet.delay();
    }

    public static void clearQueue() {
        packetQueue.clear();
        totalQueueDelay = 0;
    }

    public static int getQueueSize() {
        return packetQueue.size();
    }

    public static int getQueueDelay() {
        return totalQueueDelay;
    }

    // new Thread(OperationQueue::processQueue).start();
    private static void queueProcessingThread() {
        while (true) {
            try {
                // Retrieve item from queue
                PendingPacket packet = packetQueue.take();
                totalQueueDelay -= packet.delay();

                // TODO: Validate user is holding correct item

                // Send packet and wait for specified delay
                sendPacket(packet);
                Thread.sleep(packet.delay());

            } catch (InterruptedException ignored) {}
        }
    }

    // Sends a single packet to break or fill an area of bits from (x1, y1, z1) to (x2, y2, z2) ([xyz]1 must be < [xyz]2 for each axis)
    private static void sendPacket(PendingPacket packet) {

        // Create packet
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());

        // Recenter to player's current position if required
        if (packet.shouldRecenter()) {
            BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
            packetBuf.writeBlockPos(playerPos);
            packetBuf.writeInt(packet.x1() + 16 * (packet.origin().getX() - playerPos.getX()));
            packetBuf.writeInt(packet.y1() + 16 * (packet.origin().getY() - playerPos.getY()));
            packetBuf.writeInt(packet.z1() + 16 * (packet.origin().getZ() - playerPos.getZ()));
            packetBuf.writeInt(packet.x2() + 16 * (packet.origin().getX() - playerPos.getX()));
            packetBuf.writeInt(packet.y2() + 16 * (packet.origin().getY() - playerPos.getY()));
            packetBuf.writeInt(packet.z2() + 16 * (packet.origin().getZ() - playerPos.getZ()));
        }

        // Otherwise use origin provided
        else {
            packetBuf.writeBlockPos(packet.origin());
            packetBuf.writeInt(packet.x1());
            packetBuf.writeInt(packet.y1());
            packetBuf.writeInt(packet.z1());
            packetBuf.writeInt(packet.x2());
            packetBuf.writeInt(packet.y2());
            packetBuf.writeInt(packet.z2());
        }

        // Send packet
        ClientPlayNetworking.send(
            new Identifier(
                "bitsandchisels",
                packet.type() == OperationType.PLACE ? "smart_chisel_packet" : "bit_packet"
            ),
        packetBuf);
    }
}
