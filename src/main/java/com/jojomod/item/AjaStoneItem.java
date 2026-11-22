package com.jojomod.item;

import com.jojomod.ModAttachments;
import com.jojomod.ModItems;
import com.jojomod.data.JojoData;
import com.jojomod.util.Awards;
import com.jojomod.util.Combat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Pedra Vermelha de Aja: com a Mascara equipada e sendo vampiro, remove a fraqueza ao sol. */
public class AjaStoneItem extends Item {
    public AjaStoneItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        JojoData d = sp.getData(ModAttachments.JOJO);
        if (d.aja) {
            Combat.msg(sp, "aja_already");
            return InteractionResultHolder.fail(stack);
        }
        if (!d.vampire || !sp.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STONE_MASK.get())) {
            Combat.msg(sp, "aja_need");
            return InteractionResultHolder.fail(stack);
        }

        d.aja = true;
        if (!sp.isCreative()) stack.shrink(1);
        ServerLevel sl = (ServerLevel) level;
        sl.sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
        level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 0.5F);
        Combat.chat(sp, "aja_used");
        Awards.grant(sp, "red_stone");
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jojomod.red_stone_of_aja.tooltip").withStyle(ChatFormatting.RED));
    }
}
