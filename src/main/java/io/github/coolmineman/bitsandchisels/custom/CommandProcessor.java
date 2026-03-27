package io.github.coolmineman.bitsandchisels.custom;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class CommandProcessor implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

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
        FillHelper.fillBlocks(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ(), true, breakPacket);
        //context.getSource().sendFeedback();
        return 1;
    }

    private int cubeCommand(CommandContext<FabricClientCommandSource> context, boolean breakPacket) {
        BlockPos pos1 = CBlockPosArgumentType.getCBlockPos(context, "pos1");
        BlockPos pos2 = CBlockPosArgumentType.getCBlockPos(context, "pos2");
        FillHelper.fillBlocks(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), true, breakPacket);
        return 1;
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
            .append(Text.literal("Sets or deletes the block at the specified coordinates\n"))
            .append(Text.literal("/bc cube <set|del> <x1> <y1> <z1> <x2> <y2> <z2>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a cube defined by two corners\n"))
            .append(Text.literal("/bc sphere <set|del> <x1> <y1> <z1> <r0> <r1>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a sphere defined by a center, inner radius, and outer radius\n"))
            .append(Text.literal("/bc pillar <set|del> <number>\n").formatted(Formatting.AQUA))
            .append(Text.literal("Sets or deletes a specified end pillar"));
        context.getSource().sendFeedback(msg);
        return 1;
    }
}
