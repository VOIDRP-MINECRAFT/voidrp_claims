package ru.voidrp.claims.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory claim index. Server-thread confined (all mutations scheduled onto
 * the main thread), so plain maps are fine. Provides O(1) chunk → claim lookup
 * for the protection handlers.
 */
public final class ClaimStore {

    private final Map<String, ClaimData> byId = new HashMap<>();
    // dimension -> (packed chunk key -> claim)
    private final Map<String, Map<Long, ClaimData>> byChunk = new HashMap<>();

    public static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    public static int chunkOf(int blockCoord) {
        return Math.floorDiv(blockCoord, 16);
    }

    public void loadAll(Collection<ClaimData> claims) {
        byId.clear();
        byChunk.clear();
        for (ClaimData c : claims) {
            put(c);
        }
    }

    public void put(ClaimData claim) {
        ClaimData old = byId.put(claim.id(), claim);
        if (old != null) {
            unindex(old);
        }
        index(claim);
    }

    public void remove(String id) {
        ClaimData old = byId.remove(id);
        if (old != null) {
            unindex(old);
        }
    }

    public ClaimData byId(String id) {
        return byId.get(id);
    }

    public Collection<ClaimData> all() {
        return byId.values();
    }

    /** The claim owning the given chunk in the given dimension, or null. */
    public ClaimData claimAtChunk(String dimension, int chunkX, int chunkZ) {
        Map<Long, ClaimData> dim = byChunk.get(dimension);
        return dim == null ? null : dim.get(chunkKey(chunkX, chunkZ));
    }

    /** The claim owning the given block position, or null. */
    public ClaimData claimAtBlock(String dimension, int x, int z) {
        return claimAtChunk(dimension, chunkOf(x), chunkOf(z));
    }

    /** Claim whose core block sits exactly at this position, or null. */
    public ClaimData coreAt(String dimension, int x, int y, int z) {
        ClaimData c = claimAtBlock(dimension, x, z);
        if (c != null && c.coreX() == x && c.coreY() == y && c.coreZ() == z) {
            return c;
        }
        return null;
    }

    private void index(ClaimData c) {
        Map<Long, ClaimData> dim = byChunk.computeIfAbsent(c.dimension(), k -> new HashMap<>());
        int r = c.chunkRadius();
        for (int cx = c.coreChunkX() - r; cx <= c.coreChunkX() + r; cx++) {
            for (int cz = c.coreChunkZ() - r; cz <= c.coreChunkZ() + r; cz++) {
                dim.put(chunkKey(cx, cz), c);
            }
        }
    }

    private void unindex(ClaimData c) {
        Map<Long, ClaimData> dim = byChunk.get(c.dimension());
        if (dim == null) {
            return;
        }
        int r = c.chunkRadius();
        for (int cx = c.coreChunkX() - r; cx <= c.coreChunkX() + r; cx++) {
            for (int cz = c.coreChunkZ() - r; cz <= c.coreChunkZ() + r; cz++) {
                long key = chunkKey(cx, cz);
                if (dim.get(key) == c) {
                    dim.remove(key);
                }
            }
        }
    }
}
