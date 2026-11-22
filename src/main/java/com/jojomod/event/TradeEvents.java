package com.jojomod.event;

import com.jojomod.JojoMod;
import com.jojomod.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/** Comerciante ambulante: Flecha de Stand, Rokakaka e Pedra Vermelha de Aja. (A Mascara NAO e vendida.) */
@EventBusSubscriber(modid = JojoMod.MOD_ID)
public class TradeEvents {
    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 32), new ItemStack(ModItems.STAND_ARROW.get()), 1, 10, 0.05F));

        event.getRareTrades().add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 24), new ItemStack(ModItems.STAND_ARROW.get()), 1, 10, 0.05F));
        event.getRareTrades().add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 40), new ItemStack(ModItems.ROKAKAKA.get()), 1, 15, 0.05F));
        event.getRareTrades().add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 64), new ItemStack(ModItems.RED_STONE_OF_AJA.get()), 1, 30, 0.05F));
    }
}
