package io.github.coolmineman.bitsandchisels.custom;

import java.util.ArrayList;

import static io.github.coolmineman.bitsandchisels.custom.Utils.volume;

public class OperationProcessor {

    // Process a fill or break operation
    public static void processOperation(Operation operation) {

        // Split operation into sub-operations
        ArrayList<Operation> operations = splitOperation(operation);

        // Add each to packet queue
        for (Operation op : operations) {

            // Calculate delay from operation volume
            int delay = Math.max(
                    Config.minPacketSendInterval,
                    (int) ((volume(op) / Config.maxPacketVolume) * Config.maxPacketSendInterval)
            );

            // Create packet
            PendingPacket p = new PendingPacket(
                op.origin(),
                op.x1(), op.y1(), op.z1(),
                op.x2(), op.y2(), op.z2(),
                op.hand(), op.type(), op.shouldRecenter(),
                delay);

            // Add packet to queue
            PacketQueue.enqueuePacket(p);
        }
    }

    // Return the amount of times an operation would be split
    public static int getSplitCount(Operation op) {
        return splitOperation(op).size();
    }

    // Return the amount of time an operation would take
    public static int getTime(Operation op) {

        // Split operation into sub-operations
        ArrayList<Operation> ops = splitOperation(op);

        // Total time up
        int totalTime = 0;
        for (Operation subOp : ops) {

            // Calculate delay from operation volume
            totalTime += Math.max(
                    Config.minPacketSendInterval,
                    (int) ((volume(subOp) / Config.maxPacketVolume) * Config.maxPacketSendInterval)
            );
        }
        return totalTime;
    }

    // Splits an operation into small enough sub-operations to be processed effectively
    private static ArrayList<Operation> splitOperation(Operation op) {

        // Create array to hold split operations
        ArrayList<Operation> operations = new ArrayList<>();

        // Calculate sum of squares value to be checked
        int sumSquares = (op.x2() - op.x1()) * (op.x2() - op.x1())
                       + (op.y2() - op.y1()) * (op.y2() - op.y1())
                       + (op.z2() - op.z1()) * (op.z2() - op.z1());

        // Region fails server side sum of squares check or client side volume check
        if (sumSquares > 1000000 || volume(op) > Config.maxPacketVolume) {

            // Split in two along longest access (at whole block boundary)
            Operation op1;
            Operation op2;

            // Case where X axis is longest
            if (op.x2() - op.x1() >= op.y2() - op.y1() && op.x2() - op.x1() >= op.z2() - op.z1()) {
                int splitPoint = Math.round((float) (op.x1() + op.x2()) / 2 / 16) * 16;
                op1 = new Operation(op.origin(), op.x1(), op.y1(), op.z1(), splitPoint - 1, op.y2(), op.z2(), op.hand(), op.type(), op.shouldRecenter());
                op2 = new Operation(op.origin(), splitPoint, op.y1(), op.z1(), op.x2(), op.y2(), op.z2(), op.hand(), op.type(), op.shouldRecenter());
            }

            // Case where Z axis is longest
            else if (op.z2() - op.z1() >= op.x2() - op.x1() && op.z2() - op.z1() >= op.y2() - op.y1()) {
                int splitPoint = Math.round((float) (op.z1() + op.z2()) / 2 / 16) * 16;
                op1 = new Operation(op.origin(), op.x1(), op.y1(), op.z1(), op.x2(), op.y2(), splitPoint - 1, op.hand(), op.type(), op.shouldRecenter());
                op2 = new Operation(op.origin(), op.x1(), op.y1(), splitPoint, op.x2(), op.y2(), op.z2(), op.hand(), op.type(), op.shouldRecenter());
            }

            // Case where Y axis is longest
            else {
                int splitPoint = Math.round((float) (op.y1() + op.y2()) / 2 / 16) * 16;
                op1 = new Operation(op.origin(), op.x1(), op.y1(), op.z1(), op.x2(), splitPoint - 1, op.z2(), op.hand(), op.type(), op.shouldRecenter());
                op2 = new Operation(op.origin(), op.x1(), splitPoint, op.z1(), op.x2(), op.y2(), op.z2(), op.hand(), op.type(), op.shouldRecenter());
            }

            // Recurse with each of the two regions and add to the operations list
            operations.addAll(splitOperation(op1));
            operations.addAll(splitOperation(op2));
        }

        // Region is fine
        else operations.add(op);

        return operations;
    }
}
