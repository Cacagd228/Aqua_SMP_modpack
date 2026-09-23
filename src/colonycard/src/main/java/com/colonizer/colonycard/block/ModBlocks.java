package com.colonizer.colonycard.block;

import com.colonizer.colonycard.ColonyCardMod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Блоки мода. Пока один — блок этапа «Запросы короны» (открывает меню по ПКМ). */
public final class ModBlocks {
    private ModBlocks() {
    }

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ColonyCardMod.MODID);

    public static final DeferredBlock<StageBlock> STAGE_BLOCK = BLOCKS.register(
            "stage_block", StageBlock::new);
}
