package ru.voidrp.claims.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import ru.voidrp.claims.VoidRpClaims;
import ru.voidrp.claims.reg.ModContent;
import ru.voidrp.claims.store.ClaimData;

/** Anarchy protection: everything is allowed outside claims; inside a claim only
 *  the owner / trusted (or admins) may break, place and interact. PvP stays ON. */
public final class ProtectionHandlers {

    private ProtectionHandlers() {
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        String dim = Claims.dimKey(level);
        ClaimData claim = VoidRpClaims.store().claimAtBlock(dim, pos.getX(), pos.getZ());
        if (claim == null) {
            return; // anarchy outside claims
        }

        boolean privileged = claim.canBuild(Claims.nickLower(player)) || Claims.isAdmin(player);

        // Breaking the core itself: owner/admin removes the claim, others are denied.
        ClaimData core = VoidRpClaims.store().coreAt(dim, pos.getX(), pos.getY(), pos.getZ());
        if (core != null) {
            if (core.ownerNick().equals(Claims.nickLower(player)) || Claims.isAdmin(player)) {
                Claims.deleteClaim(level, core, player);
                return; // allow the break
            }
            event.setCanceled(true);
            return;
        }

        if (!privileged) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        String dim = Claims.dimKey(level);
        ClaimData claim = VoidRpClaims.store().claimAtBlock(dim, pos.getX(), pos.getZ());
        boolean isCore = event.getPlacedBlock().is(ModContent.CLAIM_CORE.get());

        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            if (claim != null && !(claim.canBuild(Claims.nickLower(player)) || Claims.isAdmin(player))) {
                event.setCanceled(true);
                return;
            }
            if (isCore && claim == null) {
                Claims.createClaim(level, pos, player);
            }
        } else if (claim != null) {
            // Non-player placement (dispensers, mobs) blocked inside claims.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        String dim = Claims.dimKey(level);

        ClaimData core = VoidRpClaims.store().coreAt(dim, pos.getX(), pos.getY(), pos.getZ());
        if (core != null && (core.ownerNick().equals(Claims.nickLower(player)) || Claims.isAdmin(player))) {
            if (Claims.heldItemId(player).equals(VoidRpClaims.config().upgradeItemId())) {
                Claims.upgradeClaim(level, core, player);
            } else {
                int span = 2 * (core.level() - 1) + 1;
                Claims.msg(player, "§dПриват §f" + core.ownerNick() + "§d · ур. " + core.level()
                        + " (" + span + "×" + span + " чанков) · доверенных: " + core.trusted().size());
            }
            event.setUseBlock(TriState.FALSE);
            event.setCanceled(true);
            return;
        }

        ClaimData claim = VoidRpClaims.store().claimAtBlock(dim, pos.getX(), pos.getZ());
        if (claim != null && !(claim.canBuild(Claims.nickLower(player)) || Claims.isAdmin(player))) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        String dim = Claims.dimKey(event.getLevel());
        event.getAffectedBlocks().removeIf(pos ->
                VoidRpClaims.store().claimAtBlock(dim, pos.getX(), pos.getZ()) != null);
    }
}
