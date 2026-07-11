package ru.voidrp.claims.game;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.DustParticleOptions;
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

    // Purple accent for the alternating white/purple wireframe (packed 0xRRGGBB).
    private static final DustParticleOptions PURPLE =
            new DustParticleOptions(0x9919E6, 1.0F);

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
                drawCube(level, player, claim, cube);
            }
        }
    }

    private static void drawCube(ServerLevel level, ServerPlayer player, ClaimData claim, Cube cube) {
        // Cubes are core-relative: (0,0,0) is centred on the core.
        double minX = claim.cubeMinX(cube), minY = claim.cubeMinY(cube), minZ = claim.cubeMinZ(cube);
        double maxX = minX + ClaimData.SIZE, maxY = minY + ClaimData.SIZE, maxZ = minZ + ClaimData.SIZE;
        double[] xs = {minX, maxX}, ys = {minY, maxY}, zs = {minZ, maxZ};
        double step = 4.0;

        // A running index across all points gives the alternating white/purple pattern.
        int[] i = {0};
        // edges along X
        for (double y : ys) for (double z : zs) for (double x = minX; x <= maxX; x += step) point(level, player, x, y, z, i);
        // edges along Y
        for (double x : xs) for (double z : zs) for (double y = minY; y <= maxY; y += step) point(level, player, x, y, z, i);
        // edges along Z
        for (double x : xs) for (double y : ys) for (double z = minZ; z <= maxZ; z += step) point(level, player, x, y, z, i);
    }

    private static void point(ServerLevel level, ServerPlayer player, double x, double y, double z, int[] i) {
        if ((i[0]++ & 1) == 0) {
            level.sendParticles(player, ParticleTypes.END_ROD, true, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        } else {
            level.sendParticles(player, PURPLE, true, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
