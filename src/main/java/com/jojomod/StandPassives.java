package com.jojomod;

import com.jojomod.data.JojoData;
import com.jojomod.util.Combat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 2 passivos por Stand, sempre ativos com o Stand invocado. p1 e gratuito, p2 precisa ser desbloqueado. */
public final class StandPassives {
    private StandPassives() {}

    private static boolean p2(JojoData d) {
        return Stands.unlocked("p2", d.standUnlocks);
    }

    private static void eff(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e, int amp) {
        p.addEffect(new MobEffectInstance(e, 60, amp, true, false));
    }

    /** A cada 1 segundo com o Stand invocado. */
    public static void tick(ServerPlayer p, JojoData d) {
        switch (d.stand) {
            case "star_platinum" -> {
                eff(p, MobEffects.DAMAGE_BOOST, 1);
                eff(p, MobEffects.MOVEMENT_SPEED, 0);
            }
            case "the_world" -> {
                eff(p, MobEffects.DAMAGE_BOOST, 1);
                // p1: aura de dominio
                for (LivingEntity e : Combat.around(p, 5)) {
                    if (e instanceof Enemy) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
                }
                // p2: regeneracao com pouca vida
                if (p2(d) && p.getHealth() < p.getMaxHealth() * 0.5F) eff(p, MobEffects.REGENERATION, 1);
            }
            case "crazy_diamond" -> {
                eff(p, MobEffects.DAMAGE_BOOST, 0);
                eff(p, MobEffects.REGENERATION, 0); // p1: pele regeneradora
                if (p2(d)) {                         // p2: conserta itens lentamente
                    mend(p.getMainHandItem());
                    mend(p.getOffhandItem());
                    for (ItemStack s : p.getArmorSlots()) mend(s);
                }
            }
            case "killer_queen" -> { }
            case "gold_experience" -> eff(p, MobEffects.REGENERATION, 0);
            case "magicians_red" -> eff(p, MobEffects.FIRE_RESISTANCE, 0);
            default -> { }
        }
    }

    private static void mend(ItemStack s) {
        if (s.isDamageableItem() && s.getDamageValue() > 0) s.setDamageValue(s.getDamageValue() - 1);
    }

    /** Quando o usuario do Stand vai receber dano. */
    public static void onIncoming(LivingIncomingDamageEvent e, ServerPlayer victim, JojoData d) {
        DamageSource src = e.getSource();
        ServerLevel lvl = (ServerLevel) victim.level();

        float f = Stands.durabilityFactor(victim);
        if (f < 1.0F) e.setAmount(e.getAmount() * f);

        switch (d.stand) {
            case "star_platinum" -> {
                // p1: agarra projeteis
                if (src.is(DamageTypeTags.IS_PROJECTILE) && lvl.random.nextDouble() < Stands.passiveChance(victim, 0.5)) {
                    Entity proj = src.getDirectEntity();
                    if (proj != null) proj.discard();
                    e.setCanceled(true);
                    lvl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.2, victim.getZ(), 15, 0.4, 0.4, 0.4, 0.2);
                    Combat.msg(victim, "caught");
                    return;
                }
                // p2: reflexos hipersonicos (esquiva de ataques corpo a corpo)
                if (p2(d) && src.getDirectEntity() instanceof LivingEntity
                        && lvl.random.nextDouble() < Stands.passiveChance(victim, 0.15)) {
                    e.setCanceled(true);
                    Combat.msg(victim, "dodged");
                }
            }
            case "killer_queen" -> {
                // p2: imune a explosoes
                if (p2(d) && src.is(DamageTypeTags.IS_EXPLOSION)) e.setAmount(e.getAmount() * 0.2F);
            }
            case "gold_experience" -> {
                // p2: choque de vida no atacante
                if (p2(d) && src.getDirectEntity() instanceof LivingEntity attacker) {
                    attacker.hurt(victim.damageSources().thorns(victim), 2.0F);
                    attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                }
            }
            default -> { }
        }
    }

    /** Quando o usuario do Stand ataca corpo a corpo. */
    public static void onMelee(ServerPlayer attacker, LivingEntity victim, JojoData d) {
        ServerLevel lvl = (ServerLevel) attacker.level();
        switch (d.stand) {
            case "killer_queen" -> {
                // p1: toque explosivo
                if (lvl.random.nextDouble() < Stands.passiveChance(attacker, 0.12)) {
                    lvl.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 1, victim.getZ(), 2, 0.2, 0.2, 0.2, 0);
                    Combat.hit(attacker, victim, 4.0F);
                }
            }
            case "magicians_red" -> {
                // p2: toque flamejante
                if (p2(d)) victim.setRemainingFireTicks(80);
            }
            default -> { }
        }
    }
}
