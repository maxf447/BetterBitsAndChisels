package io.github.coolmineman.bitsandchisels.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class CommandParser {

    private static ClientPlayerEntity player;

    public static boolean processMessage(String message) {
        if (!message.startsWith("!bc")) return true;
        String[] args = message.split(" ");

        player = MinecraftClient.getInstance().player;
        BlockPos pos = player.getBlockPos();

        if (args.length == 1) {
            printHelp("");
            return false;
        }

        switch (args[1]) {
            case "config" -> updateConfig(args);
            case "block" -> modifyBlock(args);
            case "cube" -> modifyCube(args);
            case "sphere" -> modifySphere(args);
            case "pillars" -> modifyPillars(args);
            default -> printHelp("");
        }
        return false;
    }

    // !bc config
    // !bc config key value
    public static void updateConfig(String[] args) {
        if (args.length == 2) {
            player.sendMessage(Text.literal("Current configuration:"));
            player.sendMessage(Text.literal(String.format("fullBlocksOnly: %s", CommandConfig.fullBlocksOnly)));
            player.sendMessage(Text.literal(String.format("unlockedMode: %s", CommandConfig.unlockedMode)));
            player.sendMessage(Text.literal(String.format("addedReach: %s", CommandConfig.addedReach)));
            player.sendMessage(Text.literal(String.format("maxVolumeManual: %s", CommandConfig.maxVolumeManual)));
            player.sendMessage(Text.literal(String.format("maxVolumeCommands: %s", CommandConfig.maxVolumeCommands)));
            player.sendMessage(Text.literal(String.format("maxChunkSize: %s", CommandConfig.maxChunkSize)));
        }
        else if (args.length == 4) {
            try {
                switch (args[2]) {
                    case "fullBlocksOnly" -> {
                        CommandConfig.fullBlocksOnly = Boolean.parseBoolean(args[3]);
                        player.sendMessage(Text.literal(String.format("fullBlocksOnly: %s", CommandConfig.fullBlocksOnly)));
                    }
                    case "unlockedMode" -> {
                        CommandConfig.unlockedMode = Boolean.parseBoolean(args[3]);
                        player.sendMessage(Text.literal(String.format("unlockedMode: %s", CommandConfig.unlockedMode)));
                    }
                    case "addedReach" -> {
                        CommandConfig.addedReach = Double.parseDouble(args[3]);
                        player.sendMessage(Text.literal(String.format("addedReach: %s", CommandConfig.addedReach)));
                    }
                    case "maxVolumeManual" -> {
                        CommandConfig.maxVolumeManual = Double.parseDouble(args[3]);
                        player.sendMessage(Text.literal(String.format("maxVolumeManual: %s", CommandConfig.maxVolumeManual)));
                    }
                    case "maxVolumeCommands" -> {
                        CommandConfig.maxVolumeCommands = Double.parseDouble(args[3]);
                        player.sendMessage(Text.literal(String.format("maxVolumeCommands: %s", CommandConfig.maxVolumeCommands)));
                    }
                    case "maxChunkSize" -> {
                        CommandConfig.maxChunkSize = Double.parseDouble(args[3]);
                        player.sendMessage(Text.literal(String.format("maxChunkSize: %s", CommandConfig.maxChunkSize)));
                    }
                    default -> printHelp("config");
                }
            }
            catch (NumberFormatException e) {
                player.sendMessage(Text.literal("Invalid number format!"));
            }
        }
        else {
            printHelp("config");
        }
    }

    // !bc block set|del x y z
    public static void modifyBlock(String[] args) {
        if (args.length == 6) {
            try {
                boolean breakPacket = args[2].equals("del");
                int x = parseCoord(args[3], "x");
                int y = parseCoord(args[4], "y");
                int z = parseCoord(args[5], "z");
                if (CommandConfig.maxVolumeCommands < 1)
                    player.sendMessage(Text.literal(String.format("Command exceeds maxVolumeCommands of %s!", CommandConfig.maxVolumeCommands)));
                else {
                    FillHelper.fillBlocks(player.getBlockPos(), x, y, z, x, y, z, true, breakPacket);
                    player.sendMessage(Text.literal("Success."));
                }
            }
            catch (NumberFormatException e) {
                player.sendMessage(Text.literal("Invalid number format!"));
            }
        }
        else {
            printHelp("block");
        }
    }

    // !bc cube set|del x1 y1 z1 x2 y2 z2
    public static void modifyCube(String[] args) {
        if (args.length == 9) {
            try {
                boolean breakPacket = args[2].equals("del");
                int x1 = parseCoord(args[3], "x");
                int y1 = parseCoord(args[4], "y");
                int z1 = parseCoord(args[5], "z");
                int x2 = parseCoord(args[6], "x");
                int y2 = parseCoord(args[7], "y");
                int z2 = parseCoord(args[8], "z");
                if (CommandConfig.maxVolumeCommands < (Math.abs(x2 - x1) + 1) * (Math.abs(y2 - y1) + 1) * (Math.abs(z2 - z1) + 1))
                    player.sendMessage(Text.literal(String.format("Command exceeds maxVolumeCommands of %s!", CommandConfig.maxVolumeCommands)));
                else {
                    FillHelper.fillBlocks(player.getBlockPos(), x1, y1, z1, x2, y2, z2, true, breakPacket);
                    player.sendMessage(Text.literal("Success."));
                }
            }
            catch (NumberFormatException e) {
                player.sendMessage(Text.literal("Invalid number format!"));
                printHelp("cube");
            }
        }
    }

    public static void modifySphere(String[] args) {}

    public static void modifyPillars(String[] args) {}

    public static void printHelp(String command) {
        player.sendMessage(Text.literal("Usage:"));
        if (Objects.equals(command, "config") || command.isEmpty()) {
            player.sendMessage(Text.literal("!bc config - Display config"));
            player.sendMessage(Text.literal("!bc config <key> <value> - Set config values"));
        }
        if (command.equals("block") || command.isEmpty()) {
            player.sendMessage(Text.literal("!bc block set <x> <y> <z> - Place block (requires bits in hand)"));
            player.sendMessage(Text.literal("!bc block del <x> <y> <z> - Break block (requires smart chisel in hand)"));
        }
        if (command.equals("cube") || command.isEmpty()) {
            player.sendMessage(Text.literal("!bc cube set <x1> <y1> <z1> <x2> <y2> <z2> - Place cube (requires bits in hand)"));
            player.sendMessage(Text.literal("!bc cube del <x1> <y1> <z1> <x2> <y2> <z2> - Break cube (requires smart chisel in hand)"));
        }
    }

    // Parses a coordinate either in relative format or absolute format
    public static int parseCoord(String coord, String axis) {
        int axisCoord = axis.equals("x") ? player.getBlockX() : (axis.equals("y") ? player.getBlockY() : player.getBlockZ());
        if (coord.equals("~")) {
            return axisCoord;
        }
        else if (coord.startsWith("~")) {
            return Integer.parseInt(coord.substring(1)) + axisCoord;
        }
        else return Integer.parseInt(coord);
    }
}
