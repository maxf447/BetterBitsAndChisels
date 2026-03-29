package io.github.coolmineman.bitsandchisels.custom;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Global queue for packets that have yet to be sent
public class PacketQueue implements ClientModInitializer {

    private static final BlockingQueue<PendingPacket> packetQueue = new LinkedBlockingQueue<>();
    private static int totalQueueDelay = 0;
    private static OperationType waitingForItem = null;

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

    public static OperationType getWaitingForItem() {
        return waitingForItem;
    }

    private static void queueProcessingThread() {
        while (true) {
            try {
                // Retrieve item from queue
                PendingPacket packet = packetQueue.take();
                totalQueueDelay -= packet.delay();

                // Pause queue if user is holding incorrect item or player is not loaded
                while (Utils.itemIncorrect(packet.type())) {
                    waitingForItem = packet.type();
                    Thread.sleep(50);
                }
                waitingForItem = null;

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

        // Write hand for bits placement packets
        if (packet.type() == OperationType.PLACE)
            packetBuf.writeBoolean(packet.hand() == Hand.MAIN_HAND);

        // Send packet
        ClientPlayNetworking.send(
            new Identifier(
                "bitsandchisels",
                packet.type() == OperationType.PLACE ? "bit_packet" : "smart_chisel_packet"
            ),
        packetBuf);
    }

    // Start queue processing thread on mod start
    @Override
    public void onInitializeClient() {
        new Thread(PacketQueue::queueProcessingThread).start();
    }
}
