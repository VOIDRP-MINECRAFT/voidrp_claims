package ru.voidrp.claims.store;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import ru.voidrp.claims.backend.ClaimDtos.ClaimDto;

/** Immutable in-memory view of a claim. Nicks are stored lowercase for matching. */
public record ClaimData(
        String id,
        String ownerNick,
        String dimension,
        int coreX,
        int coreY,
        int coreZ,
        int coreChunkX,
        int coreChunkZ,
        int level,
        Set<String> trusted
) {
    public static ClaimData fromDto(ClaimDto dto) {
        Set<String> trusted = new HashSet<>();
        if (dto.trustedNicks() != null) {
            for (String n : dto.trustedNicks()) {
                if (n != null && !n.isBlank()) {
                    trusted.add(n.toLowerCase(Locale.ROOT));
                }
            }
        }
        String owner = dto.ownerNick() == null ? "" : dto.ownerNick().toLowerCase(Locale.ROOT);
        return new ClaimData(
                dto.id(), owner, dto.dimension(),
                dto.coreX(), dto.coreY(), dto.coreZ(),
                dto.coreChunkX(), dto.coreChunkZ(),
                dto.level(), trusted
        );
    }

    /** Owner or trusted may build / interact inside this claim. */
    public boolean canBuild(String nickLower) {
        return nickLower != null && (nickLower.equals(ownerNick) || trusted.contains(nickLower));
    }

    /** Chunk radius protected around the core chunk: level L → (2L-1)² square. */
    public int chunkRadius() {
        return Math.max(0, level - 1);
    }
}
