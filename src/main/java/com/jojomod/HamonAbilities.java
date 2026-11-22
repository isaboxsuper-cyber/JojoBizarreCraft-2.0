package com.jojomod;

import com.jojomod.data.JojoData;
import com.jojomod.util.Combat;
import com.jojomod.util.Cooldowns;
import com.jojomod.util.Scheduler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HamonAbilities {
    /** Jogador -> tick ate o qual pode andar sobre a agua. */
    public static final Map<UUID, Long> WATER_WALK = new HashMap<>();

    private HamonAbilities() {}

    public static String bar(int energy) {
        int filled = Math.max(0, Math.min(10, energy / 10));
        return "§6Hamon §e" + "█".repeat(filled) + "§8" + "▒".repeat(10 - filled) + " §f" + energy + "/100";
    }

    private static boolean has(ServerPlayer p, JojoData d, int cost) {
        if (p.isCreative() || d.hamonEnergy >= cost) return true;
        Combat.msg(p, "no_energy", bar(d.hamonEnergy));
        return false;
    }

    private static void pay(ServerPlayer p, JojoData d, int cost) {
        if (!p.isCreative()) d.hamonEnergy -= cost;
    }

    public static void run(ServerPlayer p, String key) {
        JojoData d = p.getData(ModAttachments.JOJO);
        ServerLevel lvl = (ServerLevel) p.level();

        switch (key) {
            case "overdrive" -> {
                LivingEntity t = Combat.target(p, 5);
                if (t == null) { Combat.msg(p, "no_target"); return; }
                if (!has(p, d, 25) || !Cooldowns.ready(p, "hamon.overdrive", 20)) return;
                pay(p, d, 25);
                Combat.shout(p, "hamon.overdrive");
                boolean evil = Combat.isEvil(t);
                Combat.hit(p, t, evil ? 16.0F : 7.0F);
                if (evil) t.setRemainingFireTicks(120);
                t.knockback(0.8, p.getX() - t.getX(), p.getZ() - t.getZ());
                lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, t.getX(), t.getY() + 1, t.getZ(), 30, 0.4, 0.6, 0.4, 0.3);
                lvl.sendParticles(ParticleTypes.GLOW, t.getX(), t.getY() + 1, t.getZ(), 15, 0.4, 0.6, 0.4, 0.1);
                lvl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4F, 1.6F);
            }
            case "zoom_punch" -> {
                LivingEntity t = Combat.target(p, 9);
                if (t == null) { Combat.msg(p, "no_target"); return; }
                if (!has(p, d, 30) || !Cooldowns.ready(p, "hamon.zoom_punch", 80)) return;
                pay(p, d, 30);
                Combat.shout(p, "hamon.zoom_punch");
                Combat.line(p, ParticleTypes.ELECTRIC_SPARK, 9);
                boolean evil = Combat.isEvil(t);
                Combat.hit(p, t, evil ? 18.0F : 9.0F);
                if (evil) t.setRemainingFireTicks(100);
                t.knockback(1.2, p.getX() - t.getX(), p.getZ() - t.getZ());
                lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.7F);
            }
            case "turquoise" -> {
                if (!has(p, d, 35) || !Cooldowns.ready(p, "hamon.turquoise", 160)) return;
                pay(p, d, 35);
                Combat.shout(p, "hamon.turquoise");
                p.heal(8.0F);
                p.removeEffect(MobEffects.POISON);
                p.removeEffect(MobEffects.WITHER);
                lvl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 8, 0.4, 0.5, 0.4, 0.05);
                for (LivingEntity e : Combat.around(p, 6)) {
                    if (e instanceof Player ally) {
                        ally.heal(6.0F);
                        ally.removeEffect(MobEffects.POISON);
                        lvl.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1.2, ally.getZ(), 5, 0.3, 0.4, 0.3, 0.05);
                    }
                }
                lvl.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.5F);
            }
            case "sendo_kick" -> {
                if (!has(p, d, 40) || !Cooldowns.ready(p, "hamon.sendo_kick", 140)) return;
                pay(p, d, 40);
                Combat.shout(p, "hamon.sendo_kick");
                for (LivingEntity e : Combat.cone(p, 6, 0.6)) {
                    boolean evil = Combat.isEvil(e);
                    Combat.hit(p, e, evil ? 14.0F : 8.0F);
                    if (evil) e.setRemainingFireTicks(100);
                    e.knockback(2.0, p.getX() - e.getX(), p.getZ() - e.getZ());
                }
                Combat.line(p, ParticleTypes.GLOW, 6);
                lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            case "water_walk" -> {
                if (!has(p, d, 20) || !Cooldowns.ready(p, "hamon.water_walk", 200)) return;
                pay(p, d, 20);
                Combat.shout(p, "hamon.water_walk");
                WATER_WALK.put(p.getUUID(), Scheduler.now() + 600);
                Combat.chat(p, "water_walk_on");
            }
            default -> { }
        }
    }

    /** Anda sobre a agua enquanto o efeito estiver ativo (chamado todo tick). */
    public static void waterTick(ServerPlayer p, ServerLevel lvl) {
        Long until = WATER_WALK.get(p.getUUID());
        if (until == null) return;
        if (until <= Scheduler.now()) {
            WATER_WALK.remove(p.getUUID());
            Combat.msg(p, "water_walk_off");
            return;
        }
        if (p.isShiftKeyDown()) return;
        net.minecraft.core.BlockPos feet = p.blockPosition();
        boolean inWater = lvl.getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER);
        boolean onSurface = !inWater && lvl.getFluidState(feet.below()).is(net.minecraft.tags.FluidTags.WATER);
        net.minecraft.world.phys.Vec3 v = p.getDeltaMovement();
        if (inWater) {
            p.setDeltaMovement(v.x, 0.18, v.z);
            p.hurtMarked = true;
            p.fallDistance = 0;
        } else if (onSurface && v.y < 0) {
            p.setDeltaMovement(v.x, 0, v.z);
            p.hurtMarked = true;
            p.fallDistance = 0;
        }
    }
}
