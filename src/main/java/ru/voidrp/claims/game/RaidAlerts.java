package ru.voidrp.claims.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import ru.voidrp.claims.VoidRpClaims;
import ru.voidrp.claims.store.ClaimData;

/** Raid warnings: when a non-member starts mining a claim's core, or an explosion
 *  breaches claimed blocks, the (online) owner gets a throttled chat alert. Purely
 *  informational — nothing is cancelled here (anarchy raids stay allowed). */
public final class RaidAlerts {

    /** One alert per claim per this window, to avoid chat spam during a raid. */
    private static final long COOLDOWN_MS = 30_000L;

    private static final Map<String, Long> lastAlert = new HashMap<>();

    private RaidAlerts() {
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        BlockPos pos = event.getPos();
        String dim = Claims.dimKey(level);
        ClaimData core = VoidRpClaims.store().coreAt(dim, pos.getX(), pos.getY(), pos.getZ());
        if (core == null) {
            return;
        }
        String nick = Claims.nickLower(attacker);
        if (core.ownerNick().equals(nick) || core.trusted().contains(nick) || Claims.isAdmin(attacker)) {
            return; // members hitting their own core (e.g. to remove it) — no alarm
        }
        alert(level, core, "§c⚠ Игрок §f" + Claims.nickRaw(attacker)
                + "§c атакует ядро твоего привата на §f" + fmt(pos.getX(), pos.getY(), pos.getZ()) + "§c!");
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        String dim = Claims.dimKey(level);
        Set<String> hit = new HashSet<>();
        for (BlockPos pos : event.getAffectedBlocks()) {
            ClaimData claim = VoidRpClaims.store().claimAtBlock(dim, pos.getX(), pos.getY(), pos.getZ());
            if (claim != null && hit.add(claim.id())) {
                Vec3 c = event.getExplosion().center();
                alert(level, claim, "§c⚠ Твой приват взрывают у §f"
                        + fmt((int) c.x, (int) c.y, (int) c.z) + "§c!");
            }
        }
    }

    private static void alert(ServerLevel level, ClaimData claim, String message) {
        long now = System.currentTimeMillis();
        Long prev = lastAlert.get(claim.id());
        if (prev != null && now - prev < COOLDOWN_MS) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayerByName(claim.ownerNick());
        if (owner == null) {
            return; // owner offline — nothing to notify (no backend push for raids)
        }
        lastAlert.put(claim.id(), now);
        Claims.msg(owner, message);
    }

    private static String fmt(int x, int y, int z) {
        return x + " " + y + " " + z;
    }
}
