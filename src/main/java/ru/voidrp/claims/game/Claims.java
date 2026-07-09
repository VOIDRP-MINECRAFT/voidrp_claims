package ru.voidrp.claims.game;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import ru.voidrp.claims.store.Cube;

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
        if (VoidRpClaims.store().claimAtBlock(dim, pos.getX(), pos.getY(), pos.getZ()) != null) {
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

    /** Removes a claim without a player message — used when the core is blown up (raid). */
    public static void deleteClaimQuiet(ServerLevel level, ClaimData claim) {
        var server = level.getServer();
        VoidRpClaims.backend().deleteAsync(claim.id())
                .thenAccept(resp -> server.execute(() -> VoidRpClaims.store().remove(claim.id())))
                .exceptionally(ex -> {
                    server.execute(() -> VoidRpClaims.store().remove(claim.id()));
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

    /** Cost (in upgrade items) to add the next cube when the claim is at {@code level}.
     *  Scales up: 1, then +1 every 3 levels. */
    public static int upgradeCost(int level) {
        int base = Math.max(1, VoidRpClaims.config().upgradeItemsPerLevel());
        return base * (1 + Math.max(0, level - 1) / 3);
    }

    public static void upgradeClaim(ServerLevel level, ClaimData claim, ServerPlayer player, Direction face) {
        int max = VoidRpClaims.config().maxLevel();
        if (claim.level() >= max) {
            msg(player, "§eМаксимальный уровень (" + max + ") уже достигнут.");
            return;
        }

        String need = VoidRpClaims.config().upgradeItemId();
        int cost = upgradeCost(claim.level());
        ItemStack held = player.getMainHandItem();
        if (!heldItemId(player).equals(need) || held.getCount() < cost) {
            msg(player, Component.literal("§cДля улучшения нужно " + cost + "× ")
                    .append(upgradeItemName())
                    .append(Component.literal(" в руке (кликни грань ядра в нужную сторону).")));
            return;
        }

        // Walk from the core cube along the clicked face until the first free cell.
        Cube cur = claim.coreCube();
        Cube step = new Cube(face.getStepX(), face.getStepY(), face.getStepZ());
        while (claim.cubes().contains(cur)) {
            cur = new Cube(cur.x() + step.x(), cur.y() + step.y(), cur.z() + step.z());
        }
        final Cube target = cur;

        var server = level.getServer();
        VoidRpClaims.backend().addCubeAsync(claim.id(), target.x(), target.y(), target.z())
                .thenAccept(resp -> server.execute(() -> {
                    if (resp != null && resp.ok() && resp.claim() != null) {
                        held.shrink(cost);
                        VoidRpClaims.store().put(ClaimData.fromDto(resp.claim()));
                        msg(player, "§aПриват расширен: +1 куб 16×16×16 (" + resp.claim().level()
                                + "/" + max + "). Следующее улучшение — " + upgradeCost(resp.claim().level()) + "×.");
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
