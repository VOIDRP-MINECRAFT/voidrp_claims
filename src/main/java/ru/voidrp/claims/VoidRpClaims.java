package ru.voidrp.claims;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.voidrp.claims.backend.ClaimsBackendClient;
import ru.voidrp.claims.command.ClaimCommands;
import ru.voidrp.claims.config.ClaimsConfig;
import ru.voidrp.claims.game.ClaimSyncHooks;
import ru.voidrp.claims.game.ParticleHooks;
import ru.voidrp.claims.game.ProtectionHandlers;
import ru.voidrp.claims.reg.ModContent;
import ru.voidrp.claims.store.ClaimStore;

@Mod(VoidRpClaims.MODID)
public final class VoidRpClaims {

    public static final String MODID = "voidrp_claims";
    public static final Logger LOGGER = LoggerFactory.getLogger("VoidRpClaims");

    private static ClaimsConfig config;
    private static ClaimsBackendClient backend;
    private static final ClaimStore STORE = new ClaimStore();

    public VoidRpClaims(IEventBus modBus) {
        config = ClaimsConfig.load();
        backend = new ClaimsBackendClient(config);

        ModContent.register(modBus);
        NeoForge.EVENT_BUS.register(ProtectionHandlers.class);
        NeoForge.EVENT_BUS.register(ClaimSyncHooks.class);
        NeoForge.EVENT_BUS.register(ClaimCommands.class);
        NeoForge.EVENT_BUS.register(ParticleHooks.class);

        LOGGER.info("VoidRP Claims loaded — backend={}, maxLevel={}", config.backendBaseUrl(), config.maxLevel());
    }

    public static ClaimsConfig config() {
        return config;
    }

    public static ClaimsBackendClient backend() {
        return backend;
    }

    public static ClaimStore store() {
        return STORE;
    }
}
