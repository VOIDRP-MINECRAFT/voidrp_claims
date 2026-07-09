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

    /** Owner or trusted may build / interact inside this claim. */
    public boolean canBuild(String nickLower) {
        return nickLower != null && (nickLower.equals(ownerNick) || trusted.contains(nickLower));
    }

    public Cube coreCube() {
        return Cube.ofBlock(coreX, coreY, coreZ);
    }
}
