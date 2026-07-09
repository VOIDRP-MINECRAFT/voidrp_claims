package ru.voidrp.claims.game;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import ru.voidrp.claims.VoidRpClaims;
import ru.voidrp.claims.store.ClaimData;

/** Loads all claims for this server from the backend into the in-memory store
 *  when the server starts. */
public final class ClaimSyncHooks {

    private ClaimSyncHooks() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        var server = event.getServer();
        VoidRpClaims.backend().listAsync()
                .thenAccept(resp -> server.execute(() -> {
                    List<ClaimData> data = new ArrayList<>();
                    if (resp != null && resp.claims() != null) {
                        for (var dto : resp.claims()) {
                            data.add(ClaimData.fromDto(dto));
                        }
                    }
                    VoidRpClaims.store().loadAll(data);
                    VoidRpClaims.LOGGER.info("Loaded {} claim(s) from backend.", data.size());
                }))
                .exceptionally(ex -> {
                    VoidRpClaims.LOGGER.warn("Failed to load claims from backend: {}", ex.getMessage());
                    return null;
                });
    }
}
