package ru.voidrp.claims.game;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.voidrp.claims.VoidRpClaims;
import ru.voidrp.claims.store.ClaimData;

/** Beacon-like particle plume above every claim core near a player, so claims
 *  are easy to spot on the anarchy map. Runs ~every 15 ticks, skips cores with
 *  no player within 64 blocks. */
public final class ParticleHooks {

    private static int tick;

    private ParticleHooks() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tick % 15 != 0) {
            return;
        }
        if (VoidRpClaims.store().all().isEmpty()) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            String dim = level.dimension().identifier().toString();
            for (ClaimData c : VoidRpClaims.store().all()) {
                if (!c.dimension().equals(dim)) {
                    continue;
                }
                double x = c.coreX() + 0.5;
                double y = c.coreY() + 1.1;
                double z = c.coreZ() + 0.5;
                if (level.getNearestPlayer(x, y, z, 64.0, false) == null) {
                    continue;
                }
                level.sendParticles(ParticleTypes.WITCH, x, y + 0.6, z, 4, 0.15, 0.5, 0.15, 0.01);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.05, 0.7, 0.05, 0.02);
            }
        }
    }
}
