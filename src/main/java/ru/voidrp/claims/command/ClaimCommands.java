package ru.voidrp.claims.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.voidrp.claims.VoidRpClaims;
import ru.voidrp.claims.game.Claims;
import ru.voidrp.claims.reg.ModContent;
import ru.voidrp.claims.store.ClaimData;

/** /claim info | trust <player> | untrust <player> | remove | give */
public final class ClaimCommands {

    private ClaimCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("claim")
                        .then(Commands.literal("info").executes(ClaimCommands::info))
                        .then(Commands.literal("remove").executes(ClaimCommands::remove))
                        .then(Commands.literal("trust").then(
                                Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> trust(ctx, "add"))))
                        .then(Commands.literal("untrust").then(
                                Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> trust(ctx, "remove"))))
                        .then(Commands.literal("give")
                                .requires(ClaimCommands::isOpSource)
                                .executes(ClaimCommands::give))
        );
    }

    private static boolean isOpSource(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer sp) {
            return src.getServer().getPlayerList().isOp(sp.nameAndId());
        }
        return true; // console / command blocks
    }

    private static ClaimData claimAtPlayer(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        return VoidRpClaims.store().claimAtBlock(Claims.dimKey(player.level()), pos.getX(), pos.getZ());
    }

    private static boolean isOwner(ClaimData claim, ServerPlayer player) {
        return claim.ownerNick().equals(Claims.nickLower(player)) || Claims.isAdmin(player);
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ClaimData c = claimAtPlayer(p);
        if (c == null) {
            Claims.msg(p, "§7Здесь нет привата (анархия).");
            return 0;
        }
        int span = 2 * (c.level() - 1) + 1;
        Claims.msg(p, "§dПриват §f" + c.ownerNick() + "§d · ур. " + c.level()
                + " (" + span + "×" + span + " чанков) · доверенные: "
                + (c.trusted().isEmpty() ? "нет" : String.join(", ", c.trusted())));
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ClaimData c = claimAtPlayer(p);
        if (c == null) {
            Claims.msg(p, "§cВстань в приват, который хочешь снять.");
            return 0;
        }
        if (!isOwner(c, p)) {
            Claims.msg(p, "§cТы не владелец этого привата.");
            return 0;
        }
        Claims.deleteClaim((ServerLevel) p.level(), c, p);
        return 1;
    }

    private static int trust(CommandContext<CommandSourceStack> ctx, String action) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String target = StringArgumentType.getString(ctx, "player");
        ClaimData c = claimAtPlayer(p);
        if (c == null) {
            Claims.msg(p, "§cВстань в свой приват.");
            return 0;
        }
        if (!isOwner(c, p)) {
            Claims.msg(p, "§cТы не владелец этого привата.");
            return 0;
        }
        var server = ((ServerLevel) p.level()).getServer();
        VoidRpClaims.backend().trustAsync(c.id(), target, action)
                .thenAccept(resp -> server.execute(() -> {
                    if (resp != null && resp.ok() && resp.claim() != null) {
                        VoidRpClaims.store().put(ClaimData.fromDto(resp.claim()));
                        Claims.msg(p, action.equals("add")
                                ? "§aДоверенный добавлен: " + target
                                : "§eДоверенный удалён: " + target);
                    } else {
                        Claims.msg(p, "§cОшибка: " + (resp != null ? resp.error() : "нет ответа"));
                    }
                }))
                .exceptionally(ex -> {
                    server.execute(() -> Claims.msg(p, "§cСервис приватов недоступен."));
                    return null;
                });
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        p.getInventory().add(new ItemStack(ModContent.CLAIM_CORE_ITEM.get()));
        Claims.msg(p, "§aВыдано ядро привата. Поставь его, чтобы создать приват.");
        return 1;
    }
}
