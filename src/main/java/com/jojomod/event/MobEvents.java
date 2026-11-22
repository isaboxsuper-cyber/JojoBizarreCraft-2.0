package com.jojomod.event;

import com.jojomod.JojoMod;
import com.jojomod.ModAttachments;
import com.jojomod.ModItems;
import com.jojomod.data.JojoData;
import com.jojomod.entity.VampireMobs;
import com.jojomod.util.Combat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Vampiros-mob: criados com a Mascara de Pedra (Shift + botao direito em aldeoes ou mobs hostis). */
@EventBusSubscriber(modid = JojoMod.MOD_ID)
public class MobEvents {

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        if (!p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STONE_MASK.get())) return;

        Entity target = event.getTarget();
        if (!(target instanceof PathfinderMob mob)) return;
        if (!(mob instanceof AbstractVillager || mob instanceof Enemy)) return;
        if (mob.getType().is(Tags.EntityTypes.BOSSES) || mob.getMaxHealth() > 60.0F) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (VampireMobs.is(mob)) {
            Combat.msg(p, "already_vampire_mob");
            return;
        }
        VampireMobs.convert(mob, false);
        ServerLevel lvl = (ServerLevel) mob.level();
        lvl.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        lvl.playSound(null, mob.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.5F, 1.5F);
        Combat.msg(p, "mob_vampirized");
    }

    /** Reaplica a IA ao recarregar o chunk. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof PathfinderMob mob && VampireMobs.is(mob)) VampireMobs.applyAi(mob);
    }

    /** Vampiros-mob queimam ao sol. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof PathfinderMob mob) || mob.tickCount % 20 != 0) return;
        if (mob.level().isClientSide() || !VampireMobs.is(mob)) return;
        ServerLevel lvl = (ServerLevel) mob.level();
        lvl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        if (Combat.exposedToSun(mob)) mob.setRemainingFireTicks(100);
    }
}
