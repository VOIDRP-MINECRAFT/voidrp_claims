package ru.voidrp.claims.store;

/** A 16x16x16 cube cell (chunk section): cube = floor(blockCoord / 16) per axis. */
public record Cube(int x, int y, int z) {

    public static int of(int blockCoord) {
        return Math.floorDiv(blockCoord, 16);
    }

    public static Cube ofBlock(int blockX, int blockY, int blockZ) {
        return new Cube(of(blockX), of(blockY), of(blockZ));
    }
}
