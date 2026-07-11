package ru.voidrp.claims.store;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import ru.voidrp.claims.backend.ClaimDtos.ClaimDto;

/** Immutable in-memory view of a claim. Nicks are stored lowercase for matching.
 *  The claim volume is a set of 16x16x16 cube cells. */
public record ClaimData(
        String id,
        String ownerNick,
        String dimension,
        int coreX,
        int coreY,
        int coreZ,
        int level,
        Set<Cube> cubes,
        Set<String> trusted
) {
    public static ClaimData fromDto(ClaimDto dto) {
        Set<Cube> cubes = new HashSet<>();
        if (dto.cubes() != null) {
            for (List<Integer> c : dto.cubes()) {
                if (c != null && c.size() == 3) {
                    cubes.add(new Cube(c.get(0), c.get(1), c.get(2)));
                }
            }
        }
        Set<String> trusted = new HashSet<>();
        if (dto.trustedNicks() != null) {
            for (String n : dto.trustedNicks()) {
                if (n != null && !n.isBlank()) {
                    trusted.add(n.toLowerCase(Locale.ROOT));
                }
            }
        }
        String owner = dto.ownerNick() == null ? "" : dto.ownerNick().toLowerCase(Locale.ROOT);
        return new ClaimData(dto.id(), owner, dto.dimension(),
                dto.coreX(), dto.coreY(), dto.coreZ(), dto.level(), cubes, trusted);
    }

    // Cubes are RELATIVE offsets from the core; cube (0,0,0) is a 16x16x16 volume
    // centred on the core (core - 8 .. core + 8 on each axis).
    public static final int HALF = 8;
    public static final int SIZE = 16;

    /** Owner or trusted may build / interact inside this claim. */
    public boolean canBuild(String nickLower) {
        return nickLower != null && (nickLower.equals(ownerNick) || trusted.contains(nickLower));
    }

    public int cubeMinX(Cube c) {
        return coreX - HALF + c.x() * SIZE;
    }

    public int cubeMinY(Cube c) {
        return coreY - HALF + c.y() * SIZE;
    }

    public int cubeMinZ(Cube c) {
        return coreZ - HALF + c.z() * SIZE;
    }

    /** How many cells inside the cubes' bounding box are not yet claimed — the
     *  upper bound on how many cubes {@code /claim fill} would add. */
    public int fillMissingCount() {
        if (cubes.isEmpty()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Cube c : cubes) {
            minX = Math.min(minX, c.x()); maxX = Math.max(maxX, c.x());
            minY = Math.min(minY, c.y()); maxY = Math.max(maxY, c.y());
            minZ = Math.min(minZ, c.z()); maxZ = Math.max(maxZ, c.z());
        }
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        return volume - cubes.size();
    }

    public boolean containsBlock(int x, int y, int z) {
        for (Cube c : cubes) {
            int minX = cubeMinX(c), minY = cubeMinY(c), minZ = cubeMinZ(c);
            if (x >= minX && x < minX + SIZE
                    && y >= minY && y < minY + SIZE
                    && z >= minZ && z < minZ + SIZE) {
                return true;
            }
        }
        return false;
    }
}
