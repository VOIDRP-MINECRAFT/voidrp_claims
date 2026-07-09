package ru.voidrp.claims.game;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.voidrp.claims.VoidRpClaims;
import ru.voidrp.claims.store.ClaimData;
import ru.voidrp.claims.store.Cube;

/**
 * Shows the exact claim grid as a particle wireframe of every 16x16x16 cube,
 * sent only to the toggling player. Empty-hand right-click on the core toggles
 * it; a second click turns it off. Implemented with particles (not a client
 * renderer) for robustness on 26.2.
 */
public final class ClaimVisualizer {

    // player uuid -> claim id currently being shown
    private static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();
    private static int tick;

    private ClaimVisualizer() {
    }

    public static void toggle(ServerPlayer player, ClaimData claim) {
        UUID id = player.getUUID();
        if (claim.id().equals(ACTIVE.get(id))) {
            ACTIVE.remove(id);
            Claims.msg(player, "§7Сетка привата выключена.");
        } else {
            ACTIVE.put(id, claim.id());
            Claims.msg(player, "§dСетка привата включена (кликни ядро пустой рукой ещё раз, чтобы выключить).");
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty() || ++tick % 12 != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, String>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, String> e = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(e.getKey());
            ClaimData claim = VoidRpClaims.store().byId(e.getValue());
            if (player == null || claim == null) {
                it.remove();
                continue;
            }
            if (!(player.level() instanceof ServerLevel level) || !Claims.dimKey(level).equals(claim.dimension())) {
                continue;
            }
            for (Cube cube : claim.cubes()) {
                drawCube(level, player, cube);
            }
        }
    }

    private static void drawCube(ServerLevel level, ServerPlayer player, Cube cube) {
        double minX = cube.x() * 16.0, minY = cube.y() * 16.0, minZ = cube.z() * 16.0;
        double maxX = minX + 16, maxY = minY + 16, maxZ = minZ + 16;
        double[] xs = {minX, maxX}, ys = {minY, maxY}, zs = {minZ, maxZ};
        double step = 4.0;

        // edges along X
        for (double y : ys) for (double z : zs) for (double x = minX; x <= maxX; x += step) point(level, player, x, y, z);
        // edges along Y
        for (double x : xs) for (double z : zs) for (double y = minY; y <= maxY; y += step) point(level, player, x, y, z);
        // edges along Z
        for (double x : xs) for (double y : ys) for (double z = minZ; z <= maxZ; z += step) point(level, player, x, y, z);
    }

    private static void point(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        level.sendParticles(player, ParticleTypes.END_ROD, true, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
