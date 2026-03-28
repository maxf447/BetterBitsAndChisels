package io.github.coolmineman.bitsandchisels.custom;

public class Utils {
    // Calculates the volume of an operation
    public static double volume(Operation op) {
        return (double) (op.x2() - op.x1() + 1) * (op.y2() - op.y1() + 1) * (op.z2() - op.z1() + 1) / 4096;
    }
}
