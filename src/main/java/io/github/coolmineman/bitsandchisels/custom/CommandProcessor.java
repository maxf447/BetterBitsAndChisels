package io.github.coolmineman.bitsandchisels.custom;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class CommandProcessor {

    public static void registerCommands() {

        Config.loadConfig();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("bc")
            .then(literal("block")
                    .then(literal("set")
                        .then(argument("pos", CBlockPosArgumentType.blockPos())
                            .executes(context -> blockCommand(context, OperationType.PLACE))))
                    .then(literal("del")
                        .then(argument("pos", CBlockPosArgumentType.blockPos())
                            .executes(context -> blockCommand(context, OperationType.BREAK))))

            )
            .then(literal("cube")
                .then(literal("set")
                    .then(argument("pos1", CBlockPosArgumentType.blockPos())
                        .then(argument("pos2", CBlockPosArgumentType.blockPos())
                            .executes(context -> cubeCommand(context, OperationType.PLACE)))))
                .then(literal("del")
                    .then(argument("pos1", CBlockPosArgumentType.blockPos())
                        .then(argument("pos2", CBlockPosArgumentType.blockPos())
                            .executes(context -> cubeCommand(context, OperationType.BREAK)))))

            )
            .then(literal("sphere")
                .then(literal("set")
                    .then(argument("center", CBlockPosArgumentType.blockPos())
                        .then(argument("inner radius", DoubleArgumentType.doubleArg(0, 1024))
                            .then(argument("outer radius", DoubleArgumentType.doubleArg(0, 1024))
                                .executes(context -> sphereCommand(context, OperationType.PLACE))))))
                .then(literal("del")
                        .then(argument("center", CBlockPosArgumentType.blockPos())
                            .then(argument("inner radius", DoubleArgumentType.doubleArg(0, 1024))
                                .then(argument("outer radius", DoubleArgumentType.doubleArg(0, 1024))
                                    .executes(context -> sphereCommand(context, OperationType.BREAK))))))
            )
            .then(literal("info")
                .executes(CommandProcessor::infoCommand))
        ));
    }

    private static int blockCommand(CommandContext<FabricClientCommandSource> context, OperationType type) {
        BlockPos pos = CBlockPosArgumentType.getCBlockPos(context, "pos");
        context.getSource().sendFeedback(Text.literal(
                String.format("%s block at (%s, %s, %s)", type == OperationType.BREAK ? "Deleting" : "Setting", pos.getX(), pos.getY(), pos.getZ())));
        FillUtils.fillBlocks(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ(), true, type == OperationType.BREAK);
        return 1;
    }

    private static int cubeCommand(CommandContext<FabricClientCommandSource> context, OperationType type) {
        BlockPos pos1 = CBlockPosArgumentType.getCBlockPos(context, "pos1");
        BlockPos pos2 = CBlockPosArgumentType.getCBlockPos(context, "pos2");
        if (Utils.volume(pos1, pos2) > Config.maxVolumeCommands) {
            context.getSource().sendFeedback(Text.literal(
                            String.format("Volume %s larger than maximum of %s!", Utils.volume(pos1, pos2), Config.maxVolumeCommands))
                    .formatted(Formatting.RED));
            return -1;
        }
        else {
            context.getSource().sendFeedback(Text.literal(
                String.format("%s %s blocks", type == OperationType.BREAK ? "Deleting" : "Setting", Utils.volume(pos1, pos2))));
            FillUtils.fillBlocks(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), true, type == OperationType.BREAK);
            return 1;
        }
    }

    private static int sphereCommand(CommandContext<FabricClientCommandSource> context, OperationType type) {
        BlockPos pos = CBlockPosArgumentType.getCBlockPos(context, "center");
        double r0 = DoubleArgumentType.getDouble(context, "inner radius");
        double r1 = DoubleArgumentType.getDouble(context, "outer radius");

        if ((double) 4 / 3 * Math.PI * r1 * r1 * r1 > Config.maxVolumeCommands) {
            context.getSource().sendFeedback(Text.literal(
                String.format("Volume %s larger than maximum of %s!", 4 / 3 * Math.PI * r1 * r1 * r1, Config.maxVolumeCommands))
                    .formatted(Formatting.RED));
            return -1;
        }
        else {
            context.getSource().sendFeedback(Text.literal(
                    String.format("%s sphere with inner radius %s and outer radius %s at (%s, %s, %s)", type == OperationType.BREAK ? "Deleting" : "Setting", r0, r1, pos.getX(), pos.getY(), pos.getZ())));
            FillUtils.fillSphere(pos, r0, r1, true, type == OperationType.BREAK);
            return 1;
        }
    }

    private static int infoCommand(CommandContext<FabricClientCommandSource> context) {
        Text msg = Text.empty()
            .append(Text.literal("Advanced Bits & Chisels Info\n\n").formatted(Formatting.BOLD, Formatting.YELLOW, Formatting.UNDERLINE))
            .append(Text.literal("Functionality\n\n").formatted(Formatting.GREEN, Formatting.UNDERLINE))
            .append(Text.literal("Bits and Smart Chisels can be toggled between placing/breaking individual bits and placing/breaking entire blocks by left clicking the air.\n"))
            .append(Text.literal("Bit placement selection is persistent when unsneaking and only resets on upon left clicking a block.\n"))
            .append(Text.literal("Smart Chisel placement selection now resets upon right clicking a block.\n"))
            .append(Text.literal("Smart Chisels now destroy a single bit/block by default; sneaking and left clicking allows a selection to be created (similarly to bits).\n"))
            .append(Text.literal("Bits and Smart Chisels now show a HUD which shows the dimensions, volume, and estimated number of fills required to modify the current selection.\n"))
            .append(Text.literal("Client-side limits on the size of the area selected with the bits and Smart Chisel can be unlocked in the config to allow arbitrarily large selections.\n"))
            .append(Text.literal("The origin position for bits and Smart Chisel packets is adjusted to allow actions at arbitrary distances.\n"))
            .append(Text.literal("Client-side commands to directly fill or delete a block or a region of blocks have been added (`/bc info` for information).\n"))
            .append(Text.literal("Large area modification packets are sent in multiple spaced-out chunks to avoid hitting server-side limits and mitigate server lag.\n\n"))
            .append(Text.literal("Commands\n\n").formatted(Formatting.GREEN, Formatting.UNDERLINE))
            .append(Text.literal("/bc block <set|del> <x1> <y1> <z1>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes the block at the specified coordinates\n\n"))
            .append(Text.literal("/bc cube <set|del> <x1> <y1> <z1> <x2> <y2> <z2>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a cube defined by two corners\n\n"))
            .append(Text.literal("/bc sphere <set|del> <x1> <y1> <z1> <r0> <r1>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a sphere defined by a center, inner radius, and outer radius\n\n"));
        context.getSource().sendFeedback(msg);
        return 1;
    }
}
