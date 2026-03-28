package io.github.coolmineman.bitsandchisels.custom;

import com.mojang.brigadier.context.CommandContext;
import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class CommandProcessor implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // Load config
        Config.loadConfig();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("bc")
                .then(literal("block")
                        .then(literal("set")
                            .then(argument("pos", CBlockPosArgumentType.blockPos())
                                .executes(context -> blockCommand(context, false))))
                        .then(literal("del")
                            .then(argument("pos", CBlockPosArgumentType.blockPos())
                                .executes(context -> blockCommand(context, true))))

                )
                .then(literal("cube")
                    .then(literal("set")
                        .then(argument("pos1", CBlockPosArgumentType.blockPos())
                            .then(argument("pos2", CBlockPosArgumentType.blockPos())
                                .executes(context -> cubeCommand(context, false)))))
                    .then(literal("del")
                        .then(argument("pos1", CBlockPosArgumentType.blockPos())
                            .then(argument("pos2", CBlockPosArgumentType.blockPos())
                                .executes(context -> cubeCommand(context, true)))))

                )
                .then(literal("sphere")
                    .then(literal("set"))
                    .then(literal("del")))
                .then(literal("pillar")
                    .then(literal("set"))
                    .then(literal("del")))
                    .then(literal("info")
                        .executes(this::infoCommand))
            );
        });
    }

    private int blockCommand(CommandContext<FabricClientCommandSource> context, boolean breakPacket) {
        BlockPos pos = CBlockPosArgumentType.getCBlockPos(context, "pos");
        if (!holdingCorrect(breakPacket)) {
            context.getSource().sendFeedback(Text.literal(
                    String.format("Not holding %s!", breakPacket ? "Smart Chisel" : "bits"))
                            .formatted(Formatting.RED));
            return -1;
        }
        else {
            context.getSource().sendFeedback(Text.literal(
                    String.format("%s block at (%s, %s, %s)", breakPacket ? "Deleting" : "Setting", pos.getX(), pos.getY(), pos.getZ())));
            FillHelper.fillBlocks(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ(), true, breakPacket);
            return 1;
        }
    }

    private int cubeCommand(CommandContext<FabricClientCommandSource> context, boolean breakPacket) {
        BlockPos pos1 = CBlockPosArgumentType.getCBlockPos(context, "pos1");
        BlockPos pos2 = CBlockPosArgumentType.getCBlockPos(context, "pos2");
        if (volume(pos1, pos2) > Config.maxVolumeCommands) {
            context.getSource().sendFeedback(Text.literal(
                            String.format("Volume %s larger than maximum of %s!", volume(pos1, pos2), Config.maxVolumeCommands))
                    .formatted(Formatting.RED));
            return -1;
        }
        if (!holdingCorrect(breakPacket)) {
            context.getSource().sendFeedback(Text.literal(
                            String.format("Not holding %s!", breakPacket ? "Smart Chisel" : "bits"))
                    .formatted(Formatting.RED));
            return -1;
        }
        else {
            context.getSource().sendFeedback(Text.literal(
                String.format("%s %s blocks", breakPacket ? "Deleting" : "Setting", volume(pos1, pos2))));
            FillHelper.fillBlocks(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), true, breakPacket);
            return 1;
        }
    }

    private int sphereCommand(CommandContext<FabricClientCommandSource> context, boolean breakPacket) {
        return 0;
    }

    private int pillarsCommand(CommandContext<FabricClientCommandSource> context, boolean breakPacket) {
        return 0;
    }

    private int infoCommand(CommandContext<FabricClientCommandSource> context) {
        Text msg = Text.empty()
            .append(Text.literal("Advanced Bits & Chisels Info\n\n").formatted(Formatting.BOLD, Formatting.YELLOW, Formatting.UNDERLINE))
            .append(Text.literal("Functionality\n\n").formatted(Formatting.GREEN, Formatting.UNDERLINE))
            .append(Text.literal("TODO\n\n"))
            .append(Text.literal("Commands\n\n").formatted(Formatting.GREEN, Formatting.UNDERLINE))
            .append(Text.literal("/bc block <set|del> <x1> <y1> <z1>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes the block at the specified coordinates\n\n"))
            .append(Text.literal("/bc cube <set|del> <x1> <y1> <z1> <x2> <y2> <z2>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a cube defined by two corners\n\n"))
            .append(Text.literal("/bc sphere <set|del> <x1> <y1> <z1> <r0> <r1>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a sphere defined by a center, inner radius, and outer radius\n\n"))
            .append(Text.literal("/bc pillar <set|del> <number>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a specified end pillar"));
        context.getSource().sendFeedback(msg);
        return 1;
    }

    private int volume(BlockPos pos1, BlockPos pos2) {
        return (Math.abs(pos2.getX() - pos1.getX()) + 1) *
            (Math.abs(pos2.getY() - pos1.getY()) + 1) *
            (Math.abs(pos2.getZ() - pos1.getZ()) + 1);
    }

    private boolean holdingCorrect(boolean breakPacket) {
        Item item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        return (breakPacket && item == BitsAndChisels.SMART_CHISEL) ||
            (!breakPacket && item == BitsAndChisels.BIT_ITEM);
    }
}
