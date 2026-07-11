package ru.voidrp.claims.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory claim index. Server-thread confined. Claims are core-relative cube
 * volumes (not aligned to a global grid), so lookups iterate the dimension's
 * claims and test containment — fine at anarchy scale.
 */
public final class ClaimStore {

    private final Map<String, ClaimData> byId = new HashMap<>();
    private final Map<String, List<ClaimData>> byDim = new HashMap<>();

    public void loadAll(Collection<ClaimData> claims) {
        byId.clear();
        byDim.clear();
        for (ClaimData c : claims) {
            put(c);
        }
    }

    public void put(ClaimData claim) {
        ClaimData old = byId.put(claim.id(), claim);
        if (old != null) {
            dimList(old.dimension()).remove(old);
        }
        dimList(claim.dimension()).add(claim);
    }

    public void remove(String id) {
        ClaimData old = byId.remove(id);
        if (old != null) {
            dimList(old.dimension()).remove(old);
        }
    }

    public ClaimData byId(String id) {
        return byId.get(id);
    }

    public Collection<ClaimData> all() {
        return byId.values();
    }

    /** The claim containing the given block, or null. */
    public ClaimData claimAtBlock(String dimension, int x, int y, int z) {
        List<ClaimData> list = byDim.get(dimension);
        if (list == null) {
            return null;
        }
        for (ClaimData c : list) {
            if (c.containsBlock(x, y, z)) {
                return c;
            }
        }
        return null;
    }

    /** Claim whose core block sits exactly at this position, or null. */
    public ClaimData coreAt(String dimension, int x, int y, int z) {
        List<ClaimData> list = byDim.get(dimension);
        if (list == null) {
            return null;
        }
        for (ClaimData c : list) {
            if (c.coreX() == x && c.coreY() == y && c.coreZ() == z) {
                return c;
            }
        }
        return null;
    }

    private List<ClaimData> dimList(String dim) {
        return byDim.computeIfAbsent(dim, k -> new ArrayList<>());
    }
}
