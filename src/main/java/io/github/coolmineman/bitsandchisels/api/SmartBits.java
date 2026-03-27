import io.netty.buffer.Unpooled;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class SmartBits {

    // Breaks a cuboid between blocks (x1, y1, z1) and (x2, y2, z2) from position pos
    // pos must be near the player
    public static void breakCuboid(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2) {

        // Find min/max corners relative to root position
        int minX = Math.min(x1, x2) - pos.getX();
        int minY = Math.min(y1, y2) - pos.getY();
        int minZ = Math.min(z1, z2) - pos.getZ();
        int maxX = Math.max(x1, x2) - pos.getX();
        int maxY = Math.max(y1, y2) - pos.getY();
        int maxZ = Math.max(z1, z2) - pos.getZ();

        // Convert to bits positions
        minX = minX * 16;
        minY = minY * 16;
        minZ = minZ * 16;
        maxX = maxX * 16 - 1;
        maxY = maxY * 16 - 1;
        maxZ = maxZ * 16 - 1;

        // Create break packet
        PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer());
        packet.writeBlockPos(pos);
        packet.writeInt(minX);
        packet.writeInt(minY);
        packet.writeInt(minZ);
        packet.writeInt(maxX);
        packet.writeInt(maxY);
        packet.writeInt(maxZ);

        // Send break packet
        ClientPlayNetworking.send(new Identifier("bitsandchisels", "smart_chisel_packet"), packet);
    }
}