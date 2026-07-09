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
                    // High hardness (slow to mine), but modest blast resistance so the
                    // core can be destroyed by explosions — the anarchy raid path.
                    .strength(50.0F, 12.0F)
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
