package ru.voidrp.claims.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory claim index. Server-thread confined. Provides O(1) cube → claim
 * lookup (16x16x16 cells) for the protection handlers.
 */
public final class ClaimStore {

    private final Map<String, ClaimData> byId = new HashMap<>();
    // dimension -> (cube -> claim)
    private final Map<String, Map<Cube, ClaimData>> byCube = new HashMap<>();

    public void loadAll(Collection<ClaimData> claims) {
        byId.clear();
        byCube.clear();
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

    /** The claim owning the cube containing the given block, or null. */
    public ClaimData claimAtBlock(String dimension, int x, int y, int z) {
        Map<Cube, ClaimData> dim = byCube.get(dimension);
        return dim == null ? null : dim.get(Cube.ofBlock(x, y, z));
    }

    /** Claim whose core block sits exactly at this position, or null. */
    public ClaimData coreAt(String dimension, int x, int y, int z) {
        ClaimData c = claimAtBlock(dimension, x, y, z);
        if (c != null && c.coreX() == x && c.coreY() == y && c.coreZ() == z) {
            return c;
        }
        return null;
    }

    private void index(ClaimData c) {
        Map<Cube, ClaimData> dim = byCube.computeIfAbsent(c.dimension(), k -> new HashMap<>());
        for (Cube cube : c.cubes()) {
            dim.put(cube, c);
        }
    }

    private void unindex(ClaimData c) {
        Map<Cube, ClaimData> dim = byCube.get(c.dimension());
        if (dim == null) {
            return;
        }
        for (Cube cube : c.cubes()) {
            if (dim.get(cube) == c) {
                dim.remove(cube);
            }
        }
    }
}
