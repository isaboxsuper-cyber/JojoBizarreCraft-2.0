package com.jojomod.item;

import com.jojomod.Config;
import com.jojomod.ModAttachments;
import com.jojomod.Stands;
import com.jojomod.data.JojoData;
import com.jojomod.util.Awards;
import com.jojomod.util.Combat;
import java.util.List;
import java.util.ArrayList;
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

public class StandArrowItem extends Item {
    public StandArrowItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        JojoData d = sp.getData(ModAttachments.JOJO);
        if (!d.stand.isEmpty()) {
            Combat.msg(sp, "already_stand", Component.translatable("stand.jojomod." + d.stand));
            return InteractionResultHolder.fail(stack);
        }
        if (sp.getHealth() <= 8.0F && !sp.isCreative()) {
            Combat.msg(sp, "too_weak");
            return InteractionResultHolder.fail(stack);
        }

        sp.hurt(sp.damageSources().generic(), 6.0F);
        level.playSound(null, sp.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 0.6F);
        if (!sp.isCreative()) stack.shrink(1);

        if (!sp.isCreative() && level.random.nextDouble() < Config.ARROW_DEATH_CHANCE.get()) {
            Combat.chat(sp, "arrow_rejected");
            sp.kill();
            return InteractionResultHolder.consume(stack);
        }

        String id = Stands.random(level.random);
        d.stand = id;
        d.standActive = false;
        d.standUnlocks = 0;
        d.stats = new ArrayList<>(List.of(0, 0, 0, 0, 0, 0));
        level.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.7F);
        Combat.chat(sp, "stand_awakened", Component.translatable("stand.jojomod." + id));
        Combat.chat(sp, "stand_help");
        Awards.grant(sp, "stardust_crusaders");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jojomod.stand_arrow.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
