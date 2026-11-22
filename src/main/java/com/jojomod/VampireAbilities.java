package com.jojomod;

import com.jojomod.data.JojoData;
import com.jojomod.entity.VampireMobs;
import com.jojomod.util.Combat;
import com.jojomod.util.Cooldowns;
import com.jojomod.util.Scheduler;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.Vec3;

public final class VampireAbilities {
    private VampireAbilities() {}

    private static boolean hasBlood(ServerPlayer p, JojoData d, int cost) {
        if (p.isCreative() || d.blood >= cost) return true;
        Combat.msg(p, "not_enough_blood");
        return false;
    }

    private static void spend(ServerPlayer p, JojoData d, int cost) {
        if (!p.isCreative()) d.blood -= cost;
    }

    public static void run(ServerPlayer p, String key) {
        JojoData d = p.getData(ModAttachments.JOJO);
        ServerLevel lvl = (ServerLevel) p.level();

        switch (key) {
            case "drain" -> {
                LivingEntity t = Combat.target(p, 4);
                if (t == null) { Combat.msg(p, "no_victim"); return; }
                if (!Cooldowns.ready(p, "vampire.drain", 120)) return;
                Combat.shout(p, "vampire.drain");
                Combat.hit(p, t, 6.0F);
                int gain = t instanceof ServerPlayer ? 8 : t instanceof AbstractVillager ? 6 : 4;
                Combat.addBlood(p, gain);
                p.heal(4.0F);
                lvl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY() + 1.2, t.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
                lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 0.6F);
                Combat.msg(p, "blood_gain", gain);
            }
            case "eyes" -> {
                if (!hasBlood(p, d, 2) || !Cooldowns.ready(p, "vampire.eyes", 200)) return;
                spend(p, d, 2);
                Combat.shout(p, "vampire.eyes");
                LivingEntity first = Combat.target(p, 24);
                lvl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.8F);
                if (first == null) {
                    Combat.line(p, ParticleTypes.SOUL_FIRE_FLAME, 24);
                    return;
                }
                Vec3 from = p.getEyePosition();
                Vec3 hitPos = first.getBoundingBox().getCenter();
                Combat.lineBetween(lvl, from, hitPos, ParticleTypes.SOUL_FIRE_FLAME);
                Combat.hit(p, first, 14.0F);

                // Ricochete: ate 2 saltos para o alvo mais proximo
                LivingEntity current = first;
                float dmg = 9.0F;
                java.util.Set<LivingEntity> already = new java.util.HashSet<>();
                already.add(first);
                for (int i = 0; i < 2; i++) {
                    LivingEntity cur = current;
                    List<LivingEntity> near = lvl.getEntitiesOfClass(LivingEntity.class,
                            cur.getBoundingBox().inflate(8),
                            e -> e != p && e.isAlive() && !already.contains(e));
                    LivingEntity next = null;
                    double best = Double.MAX_VALUE;
                    for (LivingEntity e : near) {
                        double dist = e.distanceToSqr(cur);
                        if (dist < best) { best = dist; next = e; }
                    }
                    if (next == null) break;
                    Combat.lineBetween(lvl, cur.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(),
                            ParticleTypes.SOUL_FIRE_FLAME);
                    Combat.hit(p, next, dmg);
                    dmg -= 3.0F;
                    already.add(next);
                    current = next;
                }
            }
            case "freeze" -> {
                LivingEntity t = Combat.target(p, 8);
                if (t == null) { Combat.msg(p, "no_target"); return; }
                if (!hasBlood(p, d, 3) || !Cooldowns.ready(p, "vampire.freeze", 240)) return;
                spend(p, d, 3);
                Combat.shout(p, "vampire.freeze");
                Combat.hit(p, t, 5.0F);
                t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 160));
                if (t instanceof Mob m && !m.isNoAi()) {
                    m.setNoAi(true);
                    Scheduler.later(80, () -> { if (m.isAlive()) m.setNoAi(false); });
                } else {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 9, false, true));
                }
                lvl.sendParticles(ParticleTypes.SNOWFLAKE, t.getX(), t.getY() + 1, t.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
                lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            case "flesh_bud" -> {
                LivingEntity t = Combat.target(p, 6);
                if (!(t instanceof Zombie z)) { Combat.msg(p, "need_zombie"); return; }
                if (VampireMobs.is(z)) { Combat.msg(p, "already_ghoul"); return; }
                if (!hasBlood(p, d, 4) || !Cooldowns.ready(p, "vampire.flesh_bud", 300)) return;
                spend(p, d, 4);
                Combat.shout(p, "vampire.flesh_bud");
                VampireMobs.convert((PathfinderMob) z, true);
                lvl.sendParticles(ParticleTypes.SOUL, z.getX(), z.getY() + 1, z.getZ(), 25, 0.4, 0.8, 0.4, 0.05);
                Combat.msg(p, "ghoul_created");
            }
            default -> { }
        }
    }
}
