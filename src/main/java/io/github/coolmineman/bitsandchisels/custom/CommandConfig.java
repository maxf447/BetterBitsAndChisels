package io.github.coolmineman.bitsandchisels.custom;

public class CommandConfig {
    public static boolean fullBlocksOnly = false;   // Only select and fill full blocks
    public static boolean unlockedMode = false;     // Unlock chisel and bits selection sizes
    public static double addedReach = 0;            // Added reach when holding chisel or bits
    public static double maxVolumeManual = 1000;       // Maximum selectable volume with chisel or bits
    public static double maxVolumeCommands = 1000;     // Maximum selectable volume with commands
    public static double maxChunkSize = 10;          // Largest single chunk that can be sent to server
    public static int fillSendInterval = 100;
}
