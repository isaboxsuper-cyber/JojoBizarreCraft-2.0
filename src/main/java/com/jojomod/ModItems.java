package com.jojomod;

import com.jojomod.item.AjaStoneItem;
import com.jojomod.item.RokakakaItem;
import com.jojomod.item.StandArrowItem;
import com.jojomod.item.StoneMaskItem;
import com.jojomod.item.SunscreenItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JojoMod.MOD_ID);

    public static final DeferredItem<Item> STAND_ARROW = ITEMS.register("stand_arrow",
            () -> new StandArrowItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> STONE_MASK = ITEMS.register("stone_mask",
            () -> new StoneMaskItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> RED_STONE_OF_AJA = ITEMS.register("red_stone_of_aja",
            () -> new AjaStoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> SUNSCREEN = ITEMS.register("sunscreen",
            () -> new SunscreenItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> ROKAKAKA = ITEMS.register("rokakaka",
            () -> new RokakakaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)
                    .food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build())));

    public static final DeferredItem<Item> ZEPPELI_SPAWN_EGG = ITEMS.register("zeppeli_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ZEPPELI, 0x2B2B3A, 0xD4AF37, new Item.Properties()));
}
