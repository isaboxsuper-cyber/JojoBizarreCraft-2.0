package com.jojomod.item;

import com.jojomod.ModAttachments;
import com.jojomod.data.JojoData;
import com.jojomod.util.Awards;
import com.jojomod.util.Combat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Fruta Rokakaka: remove o seu Stand (para poder trocar por outro). */
public class RokakakaItem extends Item {
    public RokakakaItem(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            JojoData d = sp.getData(ModAttachments.JOJO);
            if (d.stand.isEmpty()) {
                Combat.msg(sp, "rokakaka_nothing");
            } else {
                d.stand = "";
                d.standActive = false;
                d.standUnlocks = 0;
                d.stats = new ArrayList<>(List.of(0, 0, 0, 0, 0, 0));
                Combat.chat(sp, "rokakaka_used");
                Awards.grant(sp, "golden_wind");
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jojomod.rokakaka.tooltip").withStyle(ChatFormatting.GOLD));
    }
}
