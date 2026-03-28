package io.github.coolmineman.bitsandchisels.custom;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class Config {
    public static boolean fullBlocksOnly = false;
    public static boolean unlockedMode = false;
    public static double addedReach = 0;
    public static int maxVolumeManual = 64;
    public static int maxVolumeCommands = 512;
    public static int maxPacketVolume = 256;
    public static int packetSendInterval = 1000;

    public static void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FabricLoader.getInstance().getConfigDir().toString() + "/advanced_bits_and_chisels.conf"))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] data = line.split(" ");
                if (data.length >= 2) {
                    switch (data[0]) {
                        case "unlockedMode:" -> unlockedMode = Boolean.parseBoolean(data[1]);
                        case "addedReach:" -> addedReach = Double.parseDouble(data[1]);
                        case "maxVolumeManual:" -> maxVolumeManual = Integer.parseInt(data[1]);
                        case "maxVolumeCommands:" -> maxVolumeCommands = Integer.parseInt(data[1]);
                        case "maxPacketVolume:" -> maxPacketVolume = Integer.parseInt(data[1]);
                        case "packetSendInterval:" -> packetSendInterval = Integer.parseInt(data[1]);
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            try (PrintWriter writer = new PrintWriter(FabricLoader.getInstance().getConfigDir().toString() + "/advanced_bits_and_chisels.conf")) {
                writer.printf("""
                        # Advanced Bits & Chisels Config
                        
                        # Whether to disable client-side size restrictions and modify packets to avoid server-side rejection
                        unlockedMode: %s
                        # Value to increase reach by when holding bits or a chisel
                        addedReach: %s
                        # Maximum volume, in blocks, modifiable manually using bits or a chisel
                        maxVolumeManual: %s
                        # Maximum volume, in blocks, modifiable with client-side commands
                        maxVolumeCommands: %s
                        # Maximum volume, in blocks, of a single packet sent to the server (high values will cause server lag)
                        maxPacketVolume: %s
                        # Interval between sending packets, in milliseconds (low values will cause server lag)
                        packetSendInterval: %s
                        """,
                        unlockedMode,
                        addedReach,
                        maxVolumeManual,
                        maxVolumeCommands,
                        maxPacketVolume,
                        packetSendInterval);
            }
            catch (IOException ignored) {}
        }
        catch (IOException | NumberFormatException ignored) {}
    }
}
