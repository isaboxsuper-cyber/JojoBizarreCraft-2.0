package com.jojomod.item;

import com.jojomod.ModAttachments;
import com.jojomod.data.JojoData;
import com.jojomod.util.Combat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Protetor solar: 2 minutos de imunidade ao sol para vampiros. */
public class SunscreenItem extends Item {
    public SunscreenItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        JojoData d = sp.getData(ModAttachments.JOJO);
        if (!d.vampire) {
            Combat.msg(sp, "sunscreen_not_vampire");
            return InteractionResultHolder.fail(stack);
        }
        d.sunscreen = 2400; // 2 minutos
        sp.clearFire();
        if (!sp.isCreative()) stack.shrink(1);
        level.playSound(null, sp.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.0F, 1.2F);
        Combat.msg(sp, "sunscreen_used");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jojomod.sunscreen.tooltip").withStyle(ChatFormatting.YELLOW));
    }
}
