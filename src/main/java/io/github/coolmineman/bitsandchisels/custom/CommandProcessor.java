package io.github.coolmineman.bitsandchisels.custom;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
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
                    .then(literal("del"))
                )
                .then(literal("pillars")
                    .then(literal("set"))
                    .then(literal("del"))
                )
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
}
