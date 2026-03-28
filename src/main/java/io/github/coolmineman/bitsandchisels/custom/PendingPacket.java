package io.github.coolmineman.bitsandchisels.custom;

import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

// Represents an operation which has yet to be sent
public record PendingPacket(BlockPos origin, int x1, int y1, int z1, int x2, int y2, int z2, Hand hand, OperationType type, boolean shouldRecenter, int delay) {}