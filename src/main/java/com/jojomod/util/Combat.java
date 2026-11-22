package com.jojomod.util;

import com.jojomod.ModAttachments;
import com.jojomod.Stands;
import com.jojomod.data.JojoData;
import com.jojomod.entity.VampireMobs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Combat {
    /** true enquanto uma habilidade causa dano (para os passivos nao se acumularem). */
    public static boolean abilityHit = false;

    private Combat() {}

    // ---------- mensagens (todas traduziveis) ----------
    public static void msg(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("msg.jojomod." + key, args), true);
    }

    public static void chat(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable("msg.jojomod." + key, args));
    }

    public static void shout(ServerPlayer p, String abilityKey) {
        p.displayClientMessage(Component.translatable("ability.jojomod." + abilityKey)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
    }

    // ---------- alvos ----------
    public static List<LivingEntity> rayDir(ServerPlayer p, Vec3 dir, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(dir.scale(range));
        List<LivingEntity> res = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().expandTowards(dir.scale(range)).inflate(1.5),
                x -> x != p && x.isAlive() && !x.isSpectator())) {
            if (e.getBoundingBox().inflate(0.35).clip(eye, end).isPresent()) res.add(e);
        }
        res.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        return res;
    }

    public static List<LivingEntity> ray(ServerPlayer p, double range) {
        return rayDir(p, p.getLookAngle(), range);
    }

    public static LivingEntity targetDir(ServerPlayer p, Vec3 dir, double range) {
        List<LivingEntity> l = rayDir(p, dir, range);
        return l.isEmpty() ? null : l.get(0);
    }

    public static LivingEntity target(ServerPlayer p, double range) {
        return targetDir(p, p.getLookAngle(), range);
    }

    public static List<LivingEntity> cone(ServerPlayer p, double range, double minDot) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range), e -> {
            if (e == p || !e.isAlive() || e.isSpectator()) return false;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            return d > 0.01 && d <= range && to.scale(1.0 / d).dot(look) >= minDot;
        });
    }

    public static List<LivingEntity> around(ServerPlayer p, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
                e -> e != p && e.isAlive() && !e.isSpectator() && e.distanceToSqr(p) <= radius * radius);
    }

    public static boolean isEvil(LivingEntity e) {
        if (e.getType().is(EntityTypeTags.UNDEAD)) return true;
        if (VampireMobs.is(e)) return true;
        return e instanceof Player pl && pl.getData(ModAttachments.JOJO).vampire;
    }

    // ---------- dano ----------
    public static void hit(ServerPlayer p, LivingEntity t, float dmg) {
        abilityHit = true;
        try {
            t.hurt(p.damageSources().playerAttack(p), dmg);
        } finally {
            abilityHit = false;
        }
    }

    public static void addBlood(ServerPlayer p, int amount) {
        JojoData d = p.getData(ModAttachments.JOJO);
        d.blood = Math.min(20, d.blood + amount);
    }

    public static boolean exposedToSun(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel lvl)) return false;
        return lvl.isDay() && !e.isInWaterRainOrBubble()
                && lvl.canSeeSky(BlockPos.containing(e.getX(), e.getEyeY(), e.getZ()));
    }

    /** Rajada de socos (ORA ORA / MUDA MUDA). Usa os atributos do Stand (Poder/Alcance). */
    public static void barrage(ServerPlayer p, int hits, float dmg, double range) {
        float d = Stands.dmg(p, dmg);
        double r = Stands.range(p, range);
        for (int i = 0; i < hits; i++) {
            Scheduler.later(1 + i * 2, () -> {
                if (!p.isAlive()) return;
                ServerLevel lvl = (ServerLevel) p.level();
                Vec3 front = p.getEyePosition().add(p.getLookAngle().scale(1.8));
                double ox = (lvl.random.nextDouble() - 0.5) * 0.9;
                double oy = (lvl.random.nextDouble() - 0.5) * 0.9;
                double oz = (lvl.random.nextDouble() - 0.5) * 0.9;
                lvl.sendParticles(ParticleTypes.CRIT, front.x + ox, front.y + oy, front.z + oz, 4, 0.2, 0.2, 0.2, 0.2);
                lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
                        0.8F, 0.8F + lvl.random.nextFloat() * 0.4F);
                LivingEntity t = target(p, r);
                if (t != null) {
                    t.invulnerableTime = 0;
                    hit(p, t, d);
                }
            });
        }
    }

    // ---------- particulas ----------
    public static void lineDir(ServerPlayer p, Vec3 dir, double length, ParticleOptions particle) {
        ServerLevel lvl = (ServerLevel) p.level();
        Vec3 eye = p.getEyePosition();
        for (double d = 0.8; d <= length; d += 0.5) {
            Vec3 pt = eye.add(dir.scale(d));
            lvl.sendParticles(particle, pt.x, pt.y - 0.15, pt.z, 1, 0, 0, 0, 0);
        }
    }

    public static void line(ServerPlayer p, ParticleOptions particle, double length) {
        lineDir(p, p.getLookAngle(), length, particle);
    }

    public static void lineBetween(ServerLevel lvl, Vec3 a, Vec3 b, ParticleOptions particle) {
        Vec3 diff = b.subtract(a);
        double len = diff.length();
        if (len < 0.01) return;
        Vec3 step = diff.scale(0.5 / len);
        Vec3 pt = a;
        for (double d = 0; d <= len; d += 0.5) {
            lvl.sendParticles(particle, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            pt = pt.add(step);
        }
    }
}
