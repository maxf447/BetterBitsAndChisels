package io.github.coolmineman.bitsandchisels.chisel;

import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import io.github.coolmineman.bitsandchisels.api.BitUtils;
import io.github.coolmineman.bitsandchisels.api.client.RedBoxCallback;
import io.github.coolmineman.bitsandchisels.custom.Config;
import io.github.coolmineman.bitsandchisels.custom.FillHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.text.DecimalFormat;

public class SmartChisel extends ToolItem implements ServerPlayNetworking.PlayChannelHandler {
    public static final Identifier PACKET_ID = new Identifier("bitsandchisels", "smart_chisel_packet");

    BlockPos block = null;

    int bitx = 0;
    int bity = 0;
    int bitz = 0;

    boolean haspos1 = false;

    private long lastBreakTick = 0;

    public SmartChisel(Settings settings) {
        super(ToolMaterials.IRON, settings);
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, this);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            Item i = player.getStackInHand(hand).getItem();
            if (i instanceof SmartChisel) {
                if (!world.isClient) return ActionResult.FAIL;
                return ((SmartChisel)i).interactBreakBlockClient(player, world, pos);
            }
            return ActionResult.PASS;
        });
    }

    public void initClient() {
        RedBoxCallback.EVENT.register((redBoxDrawer, matrixStack, vertexConsumer, worldoffsetx, worldoffsety, worldoffsetz) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player.getMainHandStack().getItem() == BitsAndChisels.SMART_CHISEL) {

                HitResult hit = client.crosshairTarget;
                if (hit.getType() == HitResult.Type.BLOCK) {
                    Direction direction = ((BlockHitResult)hit).getSide();
                    BlockPos pos = ((BlockHitResult)hit).getBlockPos();

                    int x = (int) Math.floor(((hit.getPos().getX() - pos.getX()) * 16) + (direction.getOffsetX() * -0.5d));
                    int y = (int) Math.floor(((hit.getPos().getY() - pos.getY()) * 16) + (direction.getOffsetY() * -0.5d));
                    int z = (int) Math.floor(((hit.getPos().getZ() - pos.getZ()) * 16) + (direction.getOffsetZ() * -0.5d));

                    if (!haspos1) {
                        block = pos;
                        bitx = x;
                        bity = y;
                        bitz = z;
                    }

                    // Only apply distance restriction if unlocked mode is disabled
                    if (pos.getSquaredDistance(block) > 9 && !Config.unlockedMode) return;

                    int blockXOffset = pos.getX() - block.getX();
                    int blockYOffset = pos.getY() - block.getY();
                    int blockZOffset = pos.getZ() - block.getZ();

                    int minx = Math.min(bitx, blockXOffset * 16 + x);
                    int miny = Math.min(bity, blockYOffset * 16 + y);
                    int minz = Math.min(bitz, blockZOffset * 16 + z);

                    int maxx = Math.max(bitx, blockXOffset * 16 + x);
                    int maxy = Math.max(bity, blockYOffset * 16 + y);
                    int maxz = Math.max(bitz, blockZOffset * 16 + z);

                    // Adjust to full blocks if full blocks only is enabled
                    if (Config.fullBlocksOnly) {
                        minx = Math.floorDiv(minx, 16) * 16;
                        miny = Math.floorDiv(miny, 16) * 16;
                        minz = Math.floorDiv(minz, 16) * 16;
                        maxx = Math.floorDiv(maxx, 16) * 16 + 15;
                        maxy = Math.floorDiv(maxy, 16) * 16 + 15;
                        maxz = Math.floorDiv(maxz, 16) * 16 + 15;
                    }

                    // Only draw if volume is smaller than maximum
                    double volume = (double) (maxx - minx + 1) * (maxy - miny + 1) * (maxz - minz + 1) / 4096;
                    if (volume <= Config.maxVolumeManual)
                        redBoxDrawer.drawRedBox(matrixStack, vertexConsumer, block, minx, miny, minz, maxx + 1, maxy + 1, maxz + 1, worldoffsetx, worldoffsety, worldoffsetz);

                    // Display HUD text
                    if (haspos1) {
                        DecimalFormat df = new DecimalFormat("0");
                        df.setMaximumFractionDigits(4);
                        MutableText text = Text.literal(String.format(
                                "Area: %sx%sx%s blocks, Volume: %s block%s, Fills required: %s",
                                df.format((double) (maxx - minx + 1) / 16),
                                df.format((double) (maxy - miny + 1) / 16),
                                df.format((double) (maxz - minz + 1) / 16),
                                df.format(volume),
                                volume == 1 ? "" : "s",
                                df.format(FillHelper.splitRegion(minx, miny, minz, maxx, maxy, maxz).size())
                        ));
                        client.inGameHud.setOverlayMessage(text.formatted(volume <= Config.maxVolumeManual ? Formatting.GREEN : Formatting.RED), false);
                    }
                }
            }
        });
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        int x1 = buf.readInt();
        int y1 = buf.readInt();
        int z1 = buf.readInt();
        int x2 = buf.readInt();
        int y2 = buf.readInt();
        int z2 = buf.readInt();
        server.execute(() -> {
            if (
                player.getMainHandStack().getItem() == BitsAndChisels.SMART_CHISEL &&
                player.getBlockPos().getSquaredDistance(pos) < 81 &&
                x1 <= x2 && y1 <= y2 && z1 <= z2 &&
                ((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)) + ((z2 - z1) * (z2 - z1)) <= 1000000
            ) {
                BitUtils.attemptBreakRegion(player, pos, x1, y1, z1, x2, y2, z2);
            }
        });
    }

    public ActionResult interactBreakBlockClient(PlayerEntity player, World world, BlockPos pos) {
        if (getTime() - lastBreakTick < 5) return ActionResult.CONSUME;
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult hit = client.crosshairTarget;

        if (hit.getType() == HitResult.Type.BLOCK) {
            Direction direction = ((BlockHitResult)hit).getSide();
            int x = (int) Math.floor(((hit.getPos().getX() - pos.getX()) * 16) + (direction.getOffsetX() * -0.5d));
            int y = (int) Math.floor(((hit.getPos().getY() - pos.getY()) * 16) + (direction.getOffsetY() * -0.5d));
            int z = (int) Math.floor(((hit.getPos().getZ() - pos.getZ()) * 16) + (direction.getOffsetZ() * -0.5d));
            lastBreakTick = getTime();
            // Only apply distance restriction if unlocked mode is disabled
            if (player.isSneaking() && (!haspos1 || (pos.getSquaredDistance(block) > 9 && !Config.unlockedMode))) {
                block = pos;
                bitx = x;
                bity = y;
                bitz = z;
                haspos1 = true;
            } else {

                int blockXOffset = pos.getX() - block.getX();
                int blockYOffset = pos.getY() - block.getY();
                int blockZOffset = pos.getZ() - block.getZ();

                int minx = Math.min(bitx, blockXOffset * 16 + x);
                int miny = Math.min(bity, blockYOffset * 16 + y);
                int minz = Math.min(bitz, blockZOffset * 16 + z);

                int maxx = Math.max(bitx, blockXOffset * 16 + x);
                int maxy = Math.max(bity, blockYOffset * 16 + y);
                int maxz = Math.max(bitz, blockZOffset * 16 + z);

                // Adjust origin to player position if unlocked mode is enabled
                if (Config.unlockedMode) {
                    minx += 16 * (block.getX() - player.getBlockX());
                    miny += 16 * (block.getY() - player.getBlockY());
                    minz += 16 * (block.getZ() - player.getBlockZ());
                    maxx += 16 * (block.getX() - player.getBlockX());
                    maxy += 16 * (block.getY() - player.getBlockY());
                    maxz += 16 * (block.getZ() - player.getBlockZ());
                    block = player.getBlockPos();
                }

                // Adjust to full blocks if full blocks only is enabled
                if (Config.fullBlocksOnly) {
                    minx = Math.floorDiv(minx, 16) * 16;
                    miny = Math.floorDiv(miny, 16) * 16;
                    minz = Math.floorDiv(minz, 16) * 16;
                    maxx = Math.floorDiv(maxx, 16) * 16 + 15;
                    maxy = Math.floorDiv(maxy, 16) * 16 + 15;
                    maxz = Math.floorDiv(maxz, 16) * 16 + 15;
                }

                // Only fill if volume is smaller than maximum
                double volume = (double) (maxx - minx + 1) * (maxy - miny + 1) * (maxz - minz + 1) / 4096;
                if (volume <= Config.maxVolumeManual)
                    FillHelper.fillBits(block, minx, miny, minz, maxx, maxy, maxz, true, true);
                haspos1 = false;
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.CONSUME;
    }

    // Reset selection on right click
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        haspos1 = false;
        return ActionResult.SUCCESS;
    }

    private static long getTime() {
        return System.currentTimeMillis() / 50;
    }
}
