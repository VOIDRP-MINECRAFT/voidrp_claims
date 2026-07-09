package ru.voidrp.claims.game;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import ru.voidrp.claims.VoidRpClaims;
import ru.voidrp.claims.backend.ClaimDtos.ClaimCreateRequest;
import ru.voidrp.claims.store.ClaimData;

/** Server-side helpers: dimension keys, permission/nick checks, and the
 *  backend-backed create / delete / upgrade actions. */
public final class Claims {

    private Claims() {
    }

    public static String dimKey(LevelAccessor level) {
        if (level instanceof net.minecraft.world.level.Level l) {
            return l.dimension().identifier().toString();
        }
        return "";
    }

    public static String nickLower(Player player) {
        return player.getGameProfile().name().toLowerCase(Locale.ROOT);
    }

    public static String nickRaw(Player player) {
        return player.getGameProfile().name();
    }

    public static boolean isAdmin(Player player) {
        if (player instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            return sl.getServer().getPlayerList().isOp(sp.nameAndId());
        }
        return false;
    }

    public static void msg(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    public static void msg(Player player, Component component) {
        player.sendSystemMessage(component);
    }

    /** Localized display name of the configured upgrade item (client resolves it). */
    public static Component upgradeItemName() {
        Item item = BuiltInRegistries.ITEM.getValue(
                Identifier.parse(VoidRpClaims.config().upgradeItemId()));
        return Component.translatable(item.getDescriptionId());
    }

    public static String heldItemId(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    // ── actions ──────────────────────────────────────────────────────────
    public static void createClaim(ServerLevel level, BlockPos pos, ServerPlayer player) {
        String dim = dimKey(level);
        if (VoidRpClaims.store().claimAtBlock(dim, pos.getX(), pos.getZ()) != null) {
            msg(player, "§cЗдесь уже есть приват — ядро не активировано.");
            return;
        }
        ClaimCreateRequest req = new ClaimCreateRequest(nickRaw(player), dim, pos.getX(), pos.getY(), pos.getZ(), 1);
        var server = level.getServer();
        VoidRpClaims.backend().createAsync(req)
                .thenAccept(resp -> server.execute(() -> {
                    if (resp != null && resp.ok() && resp.claim() != null) {
                        VoidRpClaims.store().put(ClaimData.fromDto(resp.claim()));
                        msg(player, Component.literal("§aПриват создан! Уровень 1 (1 чанк). Кликни по ядру предметом «")
                                .append(upgradeItemName())
                                .append(Component.literal("», чтобы расширить.")));
                    } else {
                        msg(player, "§cНе удалось создать приват: " + (resp != null ? resp.error() : "нет ответа") + ".");
                    }
                }))
                .exceptionally(ex -> {
                    server.execute(() -> msg(player, "§cСервис приватов недоступен, попробуй позже."));
                    return null;
                });
    }

    public static void deleteClaim(ServerLevel level, ClaimData claim, ServerPlayer player) {
        var server = level.getServer();
        VoidRpClaims.backend().deleteAsync(claim.id())
                .thenAccept(resp -> server.execute(() -> {
                    VoidRpClaims.store().remove(claim.id());
                    msg(player, "§eПриват снят.");
                }))
                .exceptionally(ex -> {
                    // Optimistically drop locally so it stops protecting.
                    server.execute(() -> VoidRpClaims.store().remove(claim.id()));
                    return null;
                });
    }

    public static void upgradeClaim(ServerLevel level, ClaimData claim, ServerPlayer player) {
        int max = VoidRpClaims.config().maxLevel();
        if (claim.level() >= max) {
            msg(player, "§eМаксимальный уровень (" + max + ") уже достигнут.");
            return;
        }

        String need = VoidRpClaims.config().upgradeItemId();
        int cost = Math.max(1, VoidRpClaims.config().upgradeItemsPerLevel());
        ItemStack held = player.getMainHandItem();
        if (!heldItemId(player).equals(need) || held.getCount() < cost) {
            msg(player, Component.literal("§cДля улучшения нужно " + cost + "× ")
                    .append(upgradeItemName())
                    .append(Component.literal(" в руке.")));
            return;
        }

        int newLevel = claim.level() + 1;
        var server = level.getServer();
        VoidRpClaims.backend().upgradeAsync(claim.id(), newLevel)
                .thenAccept(resp -> server.execute(() -> {
                    if (resp != null && resp.ok() && resp.claim() != null) {
                        held.shrink(cost);
                        VoidRpClaims.store().put(ClaimData.fromDto(resp.claim()));
                        int span = 2 * (newLevel - 1) + 1;
                        msg(player, "§aПриват улучшен до уровня " + newLevel + " (" + span + "×" + span + " чанков).");
                    } else {
                        msg(player, "§cУлучшение отклонено: " + (resp != null ? resp.error() : "нет ответа") + ".");
                    }
                }))
                .exceptionally(ex -> {
                    server.execute(() -> msg(player, "§cСервис приватов недоступен, попробуй позже."));
                    return null;
                });
    }
}
