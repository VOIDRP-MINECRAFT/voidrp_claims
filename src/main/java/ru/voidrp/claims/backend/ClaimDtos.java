package ru.voidrp.claims.backend;

import java.util.List;

/** Wire DTOs for the backend /claims API. Gson uses LOWER_CASE_WITH_UNDERSCORES,
 *  so Java camelCase fields map to snake_case JSON automatically. */
public final class ClaimDtos {

    private ClaimDtos() {
    }

    public record ClaimDto(
            String id,
            String ownerNick,
            String dimension,
            int coreX,
            int coreY,
            int coreZ,
            int level,
            List<List<Integer>> cubes,
            List<String> trustedNicks
    ) {
    }

    public record ClaimListResponse(List<ClaimDto> claims) {
    }

    public record ClaimCreateRequest(
            String ownerNick,
            String dimension,
            int coreX,
            int coreY,
            int coreZ,
            int level
    ) {
    }

    public record ClaimUpgradeRequest(List<Integer> cube) {
    }

    public record ClaimTrustRequest(String nick, String action) {
    }

    public record ClaimActionResponse(boolean ok, String error, ClaimDto claim) {
        public static ClaimActionResponse failed(String error) {
            return new ClaimActionResponse(false, error, null);
        }
    }
}
