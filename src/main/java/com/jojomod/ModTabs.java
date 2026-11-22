package com.jojomod;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JojoMod.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jojomod"))
                    .icon(() -> new ItemStack(ModItems.STAND_ARROW.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.STAND_ARROW.get());
                        output.accept(ModItems.STONE_MASK.get());
                        output.accept(ModItems.RED_STONE_OF_AJA.get());
                        output.accept(ModItems.SUNSCREEN.get());
                        output.accept(ModItems.ROKAKAKA.get());
                        output.accept(ModItems.ZEPPELI_SPAWN_EGG.get());
                    })
                    .build());
}
