package ru.voidrp.claims.reg;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.voidrp.claims.VoidRpClaims;

/** Registers the claim-core block + item. */
public final class ModContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoidRpClaims.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoidRpClaims.MODID);

    public static final DeferredBlock<Block> CLAIM_CORE = BLOCKS.registerSimpleBlock(
            "claim_core",
            props -> props
                    .mapColor(MapColor.COLOR_PURPLE)
                    // Explosion-proof (resistance 1200 — obsidian tier): the core can't
                    // be blown up. Very high hardness: the raid path is to MINE it out,
                    // which takes a long time (~37s with a diamond pickaxe). The
                    // needs_diamond_tool tag makes lower tiers impractical.
                    .strength(200.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 15)
    );

    public static final DeferredItem<BlockItem> CLAIM_CORE_ITEM =
            ITEMS.registerSimpleBlockItem("claim_core", CLAIM_CORE);

    private ModContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
