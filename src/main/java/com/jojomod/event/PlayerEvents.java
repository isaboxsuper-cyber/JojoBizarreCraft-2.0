package com.jojomod.event;

import com.jojomod.Config;
import com.jojomod.HamonAbilities;
import com.jojomod.JojoMod;
import com.jojomod.ModAttachments;
import com.jojomod.ModItems;
import com.jojomod.Quests;
import com.jojomod.StandAbilities;
import com.jojomod.StandPassives;
import com.jojomod.Stands;
import com.jojomod.data.JojoData;
import com.jojomod.entity.VampireMobs;
import com.jojomod.network.Sync;
import com.jojomod.util.Combat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = JojoMod.MOD_ID)
public class PlayerEvents {

    // ============ TICK ============
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        JojoData d = p.getData(ModAttachments.JOJO);
        ServerLevel lvl = (ServerLevel) p.level();
        int t = p.tickCount;

        if (d.sunscreen > 0) d.sunscreen--;

        // ---- Stand ----
        boolean out = d.standActive && Stands.exists(d.stand);
        if (d.standActive && !out) d.standActive = false;
        if (out) {
            if (t % 20 == 0) StandPassives.tick(p, d);
            if (t % 4 == 0) {
                Vec3 back = p.getLookAngle().multiply(1, 0, 1).normalize().scale(-0.9);
                ParticleOptions part = switch (d.stand) {
                    case "magicians_red" -> ParticleTypes.FLAME;
                    case "gold_experience" -> ParticleTypes.HAPPY_VILLAGER;
                    case "killer_queen" -> ParticleTypes.ELECTRIC_SPARK;
                    case "crazy_diamond" -> ParticleTypes.CHERRY_LEAVES;
                    default -> ParticleTypes.END_ROD;
                };
                lvl.sendParticles(part, p.getX() + back.x, p.getY() + 1.2, p.getZ() + back.z, 3, 0.25, 0.5, 0.25, 0.01);
            }
        }

        // ---- Hamon (somente na forma humana) ----
        if (d.hamon && !d.vampire) {
            int en = d.hamonEnergy;
            if (p.isShiftKeyDown()) {
                if (t % 5 == 0 && en < 100) {
                    d.hamonEnergy = Math.min(100, en + 2);
                    lvl.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.6, 0.4, 0.02);
                }
                if (t % 5 == 0) p.displayClientMessage(net.minecraft.network.chat.Component.literal(HamonAbilities.bar(d.hamonEnergy)), true);
            } else if (t % 40 == 0 && en < 100) {
                d.hamonEnergy = en + 1;
            }
            HamonAbilities.waterTick(p, lvl);
        }

        // ---- Vampiro ----
        if (d.vampire) vampireTick(p, d, lvl, t);

        // ---- Missoes do Zeppeli ----
        Quests.tick(p, d);

        // ---- Bites the Dust ----
        StandAbilities.historyTick(p, t);

        if (t % 5 == 0) Sync.tick(p);
    }

    private static void vampireTick(ServerPlayer p, JojoData d, ServerLevel lvl, int t) {
        if (t % 20 == 0) {
            // Sem fome: o sangue substitui a comida
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(5.0F);

            if (d.blood >= 6) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false));
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, d.blood == 0 ? 1 : 0, true, false));
                if (d.blood == 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            }
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));

            // Sol: queima mesmo de capacete; so a Pedra de Aja ou o protetor solar protegem
            if (Config.VAMPIRES_BURN.get() && !d.aja && d.sunscreen <= 0 && Combat.exposedToSun(p)) {
                p.setRemainingFireTicks(Math.max(p.getRemainingFireTicks(), 100));
                Combat.msg(p, "sun_burn");
            }
        }
        if (t % 1200 == 0 && d.blood > 0) d.blood--;
        if (d.blood == 0 && t % 80 == 0 && p.getHealth() > 2.0F && !p.isCreative()) {
            p.hurt(p.damageSources().starve(), 1.0F);
        }
    }

    // ============ DANO ============
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();

        if (victim instanceof ServerPlayer vp) {
            JojoData vd = vp.getData(ModAttachments.JOJO);
            if (vd.standActive && Stands.exists(vd.stand)) {
                StandPassives.onIncoming(event, vp, vd);
                if (event.isCanceled()) return;
            }
        }

        if (Combat.abilityHit) return;
        if (!(src.getEntity() instanceof ServerPlayer att)) return;
        if (src.getDirectEntity() != att || !src.is(DamageTypes.PLAYER_ATTACK)) return; // so corpo a corpo

        JojoData ad = att.getData(ModAttachments.JOJO);

        // Hamon: dano extra contra mortos-vivos e vampiros
        if (ad.hamon && !ad.vampire && Combat.isEvil(victim) && ad.hamonEnergy >= 4) {
            ad.hamonEnergy -= 4;
            event.setAmount(event.getAmount() * 1.5F);
            victim.setRemainingFireTicks(60);
            ((ServerLevel) victim.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(), victim.getY() + 1, victim.getZ(), 10, 0.3, 0.5, 0.3, 0.2);
        }

        // Vampiro: rouba vida
        if (ad.vampire) att.heal(event.getAmount() * 0.25F);

        // Passivos de Stand ao bater
        if (ad.standActive && Stands.exists(ad.stand)) StandPassives.onMelee(att, victim, ad);
    }

    // ============ MORTE ============
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // Bites the Dust: reviver se morrer por explosao
        if (victim instanceof ServerPlayer vp && event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && StandAbilities.isArmed(vp)) {
            event.setCanceled(true);
            StandAbilities.rewind(vp);
            return;
        }

        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            JojoData kd = killer.getData(ModAttachments.JOJO);
            if (kd.standActive && Stands.exists(kd.stand)) {
                Stands.addXp(killer, 4 + (int) (victim.getMaxHealth() / 5.0F));
            }
            if (victim.getType().is(EntityTypeTags.UNDEAD) || Combat.isEvil(victim)) Quests.onKill(killer);
        }
    }

    /** Bites the Dust: uma explosao de mob perto de quem esta armado = rebobinar o tempo. */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof net.minecraft.world.entity.Mob)) return;
        for (Object o : new java.util.ArrayList<>(event.getAffectedEntities())) {
            if (o instanceof ServerPlayer sp && StandAbilities.isArmed(sp)) {
                event.getAffectedEntities().remove(sp);
                StandAbilities.rewind(sp);
            }
        }
    }

    // ============ SANGUE: vampiros nao comem ============
    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack stack = event.getItem();
        if (!p.getData(ModAttachments.JOJO).vampire) return;
        if (stack.has(DataComponents.FOOD) && !stack.is(ModItems.ROKAKAKA.get())) {
            event.setCanceled(true);
            Combat.msg(p, "vampire_no_food");
        }
    }

    // ============ MORTE DO JOGADOR / LOGIN ============
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            JojoData old = event.getOriginal().getData(ModAttachments.JOJO);
            JojoData copy = old.copy();
            copy.standActive = false;
            copy.blood = Math.max(copy.blood, 10);
            event.getEntity().setData(ModAttachments.JOJO, copy);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) Sync.forget(sp);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            Sync.forget(sp);
            Sync.tick(sp);
        }
    }
}
