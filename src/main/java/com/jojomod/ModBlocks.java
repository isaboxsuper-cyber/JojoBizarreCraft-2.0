package com.jojomod;

import com.jojomod.block.RokakakaBushBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JojoMod.MOD_ID);

    public static final DeferredBlock<Block> ROKAKAKA_BUSH = BLOCKS.register("rokakaka_bush",
            () -> new RokakakaBushBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));
}
