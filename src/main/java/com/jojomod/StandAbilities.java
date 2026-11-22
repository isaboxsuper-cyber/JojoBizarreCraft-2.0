package com.jojomod;

import com.jojomod.data.JojoData;
import com.jojomod.util.Awards;
import com.jojomod.util.Combat;
import com.jojomod.util.Cooldowns;
import com.jojomod.util.Scheduler;
import com.jojomod.util.TimeStop;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** As 7 habilidades ativas (5 combate + 2 qualidade de vida) de cada Stand. */
public final class StandAbilities {
    private static final Map<UUID, Set<UUID>> BOMBS = new HashMap<>();
    private static final Map<UUID, Long> BTD_UNTIL = new HashMap<>();
    private record Snap(ServerLevel level, double x, double y, double z, float health) {}
    private static final Map<UUID, Deque<Snap>> HISTORY = new HashMap<>();

    private StandAbilities() {}

    public static void clearState() {
        BOMBS.clear();
        BTD_UNTIL.clear();
        HISTORY.clear();
    }

    // ---------- helpers ----------
    private static boolean ready(ServerPlayer p, String key, int ticks) {
        if (!Cooldowns.ready(p, key, Stands.cd(p, ticks))) return false;
        Combat.shout(p, key);
        Stands.addXp(p, 2);
        return true;
    }

    private static void hit(ServerPlayer p, LivingEntity t, float dmg) {
        Combat.hit(p, t, Stands.dmg(p, dmg));
    }

    private static void sound(ServerPlayer p, net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, vol, pitch);
    }

    private static void cleanHarmful(LivingEntity who) {
        for (MobEffectInstance i : new ArrayList<>(who.getActiveEffects())) {
            if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) who.removeEffect(i.getEffect());
        }
    }

    private static void effect(LivingEntity e, Holder<MobEffect> eff, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(eff, ticks, amp, false, true));
    }

    private static void boom(ServerPlayer p, double x, double y, double z, float power, LivingEntity victim, float extra) {
        ((ServerLevel) p.level()).explode(p, x, y, z, power, Level.ExplosionInteraction.NONE);
        if (victim != null && victim.isAlive()) hit(p, victim, extra);
    }

    private static void timeStop(ServerPlayer p, String key, int ticks, int cd) {
        if (TimeStop.active()) {
            Combat.msg(p, "time_busy");
            return;
        }
        if (ready(p, key, cd)) {
            TimeStop.start(p, ticks);
            Awards.grant(p, "stone_ocean");
        }
    }

    // ---------- despacho ----------
    public static void run(ServerPlayer p, String stand, String slot) {
        String key = stand + "." + slot;
        switch (key) {
            // ===== STAR PLATINUM =====
            case "star_platinum.c1" -> { if (ready(p, key, 240)) Combat.barrage(p, 12, 4.0F, 4.5); }
            case "star_platinum.c2" -> {
                if (ready(p, key, 160)) {
                    double r = Stands.range(p, 12);
                    Combat.line(p, ParticleTypes.END_ROD, r);
                    for (LivingEntity e : Combat.ray(p, r)) hit(p, e, 9.0F);
                }
            }
            case "star_platinum.c3" -> {
                if (ready(p, key, 300)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    for (LivingEntity e : Combat.around(p, Stands.range(p, 10))) {
                        Vec3 pull = p.position().subtract(e.position()).normalize().scale(1.1);
                        e.setDeltaMovement(pull.x, 0.3, pull.z);
                        e.hurtMarked = true;
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                    }
                    lvl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 40, 3, 1, 3, 0.1);
                    sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 0.6F);
                }
            }
            case "star_platinum.c4" -> timeStop(p, key, 100, 900);
            case "star_platinum.c5" -> { if (ready(p, key, 1200)) Combat.barrage(p, 30, 6.0F, 5.0); }
            case "star_platinum.q1" -> superVision(p, key);
            case "star_platinum.q2" -> grabItems(p, key);

            // ===== THE WORLD =====
            case "the_world.c1" -> { if (ready(p, key, 240)) Combat.barrage(p, 14, 4.5F, 4.5); }
            case "the_world.c2" -> {
                if (ready(p, key, 160)) {
                    double r = Stands.range(p, 18);
                    for (int i = 0; i < 5; i++) {
                        Vec3 dir = p.getLookAngle().yRot((float) Math.toRadians(-20 + i * 10));
                        Combat.lineDir(p, dir, r, ParticleTypes.CRIT);
                        LivingEntity t = Combat.targetDir(p, dir, r);
                        if (t != null) { t.invulnerableTime = 0; hit(p, t, 5.5F); }
                    }
                    sound(p, SoundEvents.ARROW_SHOOT, 1.0F, 1.4F);
                }
            }
            case "the_world.c3" -> roadRoller(p, key);
            case "the_world.c4" -> timeStop(p, key, 160, 1200);
            case "the_world.c5" -> { if (ready(p, key, 1000)) Combat.barrage(p, 20, 7.0F, 5.0); }
            case "the_world.q1" -> {
                if (ready(p, key, 600)) {
                    effect(p, MobEffects.MOVEMENT_SPEED, 600, 2);
                    effect(p, MobEffects.JUMP, 600, 1);
                }
            }
            case "the_world.q2" -> blink(p, key);

            // ===== CRAZY DIAMOND =====
            case "crazy_diamond.c1" -> { if (ready(p, key, 220)) Combat.barrage(p, 10, 4.0F, 4.5); }
            case "crazy_diamond.c2" -> {
                LivingEntity t = Combat.target(p, Stands.range(p, 5));
                if (t == null) { Combat.msg(p, "no_target"); return; }
                if (ready(p, key, 120)) {
                    hit(p, t, 9.0F);
                    t.knockback(2.0, p.getX() - t.getX(), p.getZ() - t.getZ());
                }
            }
            case "crazy_diamond.c3" -> {
                if (ready(p, key, 200)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    for (LivingEntity e : Combat.around(p, Stands.range(p, 4.5))) {
                        hit(p, e, 6.0F);
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
                    }
                    lvl.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1, p.getZ(), 60, 2.5, 1, 2.5, 0.3);
                    sound(p, SoundEvents.GLASS_BREAK, 1.2F, 0.8F);
                }
            }
            case "crazy_diamond.c4" -> {
                if (ready(p, key, 400)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    p.heal(10.0F);
                    for (LivingEntity e : Combat.around(p, Stands.range(p, 8))) {
                        if (e instanceof Player ally) {
                            ally.heal(10.0F);
                            cleanHarmful(ally);
                        } else if (e instanceof Enemy) {
                            hit(p, e, 8.0F);
                        }
                    }
                    lvl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 15, 3, 1, 3, 0.05);
                }
            }
            case "crazy_diamond.c5" -> {
                if (ready(p, key, 1000)) {
                    Combat.barrage(p, 25, 6.0F, 5.0);
                    Scheduler.later(54, () -> { if (p.isAlive()) p.heal(10.0F); });
                }
            }
            case "crazy_diamond.q1" -> {
                if (ready(p, key, 200)) {
                    LivingEntity t = Combat.target(p, Stands.range(p, 5));
                    LivingEntity who = t != null ? t : p;
                    who.heal(who.getMaxHealth());
                    cleanHarmful(who);
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.HEART, who.getX(), who.getY() + 1.2, who.getZ(), 10, 0.4, 0.5, 0.4, 0.05);
                }
            }
            case "crazy_diamond.q2" -> {
                if (ready(p, key, 600)) {
                    int n = 0;
                    List<ItemStack> all = new ArrayList<>(p.getInventory().items);
                    all.addAll(p.getInventory().armor);
                    all.addAll(p.getInventory().offhand);
                    for (ItemStack s : all) {
                        if (s.isDamageableItem() && s.getDamageValue() > 0) {
                            s.setDamageValue(0);
                            n++;
                        }
                    }
                    Combat.msg(p, "items_fixed", n);
                    sound(p, SoundEvents.ANVIL_USE, 0.6F, 1.4F);
                }
            }

            // ===== KILLER QUEEN =====
            case "killer_queen.c1" -> killerBomb(p, key);
            case "killer_queen.c2" -> {
                if (ready(p, key, 900)) {
                    for (int k = 1; k <= 6; k++) {
                        Scheduler.later(k * 30, () -> {
                            if (!p.isAlive()) return;
                            LivingEntity best = null;
                            double bestD = Double.MAX_VALUE;
                            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class,
                                    p.getBoundingBox().inflate(20), x -> x instanceof Enemy && x.isAlive())) {
                                double dist = e.distanceToSqr(p);
                                if (dist < bestD) { bestD = dist; best = e; }
                            }
                            if (best != null) boom(p, best.getX(), best.getY(0.5), best.getZ(), 2.4F, best, 6.0F);
                        });
                    }
                }
            }
            case "killer_queen.c3" -> {
                if (ready(p, key, 200)) {
                    double r = Stands.range(p, 25);
                    LivingEntity t = Combat.target(p, r);
                    Vec3 loc;
                    if (t != null) {
                        loc = t.getBoundingBox().getCenter();
                    } else {
                        HitResult hr = p.pick(r, 0.0F, false);
                        loc = hr.getLocation();
                    }
                    boom(p, loc.x, loc.y, loc.z, 3.0F, t, 6.0F);
                }
            }
            case "killer_queen.c4" -> {
                if (ready(p, key, 600)) {
                    int i = 0;
                    for (LivingEntity e : Combat.around(p, Stands.range(p, 12))) {
                        if (!(e instanceof Enemy)) continue;
                        int delay = 10 + (i++) * 4;
                        Scheduler.later(delay, () -> {
                            if (e.isAlive()) boom(p, e.getX(), e.getY(0.5), e.getZ(), 2.6F, e, 8.0F);
                        });
                    }
                }
            }
            case "killer_queen.c5" -> {
                if (ready(p, key, 6000)) {
                    BTD_UNTIL.put(p.getUUID(), Scheduler.now() + 1800);
                    HISTORY.remove(p.getUUID());
                    Combat.chat(p, "btd_armed");
                    sound(p, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.4F);
                }
            }
            case "killer_queen.q1" -> blastTunnel(p, key);
            case "killer_queen.q2" -> {
                if (ready(p, key, 200)) {
                    Vec3 v = p.getDeltaMovement();
                    p.setDeltaMovement(v.x, 1.3, v.z);
                    p.hurtMarked = true;
                    p.fallDistance = 0;
                    effect(p, MobEffects.SLOW_FALLING, 100, 0);
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 3, 0.3, 0.1, 0.3, 0);
                    sound(p, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.5F, 1.4F);
                }
            }

            // ===== GOLD EXPERIENCE =====
            case "gold_experience.c1" -> {
                LivingEntity t = Combat.target(p, Stands.range(p, 4));
                if (t == null) { Combat.msg(p, "no_target"); return; }
                if (ready(p, key, 120)) {
                    hit(p, t, 6.0F);
                    effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);
                    effect(t, MobEffects.CONFUSION, 120, 0);
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX(), t.getY() + 1, t.getZ(), 15, 0.4, 0.6, 0.4, 0.1);
                }
            }
            case "gold_experience.c2" -> {
                if (ready(p, key, 160)) {
                    double r = Stands.range(p, 12);
                    Combat.line(p, ParticleTypes.HAPPY_VILLAGER, r);
                    for (LivingEntity e : Combat.ray(p, r)) hit(p, e, e instanceof Enemy ? 8.0F : 4.0F);
                }
            }
            case "gold_experience.c3" -> {
                LivingEntity t = Combat.target(p, Stands.range(p, 8));
                if (t == null) { Combat.msg(p, "no_target"); return; }
                if (ready(p, key, 240)) {
                    hit(p, t, 3.0F);
                    effect(t, MobEffects.MOVEMENT_SLOWDOWN, 100, 6);
                    effect(t, MobEffects.WEAKNESS, 100, 2);
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.COMPOSTER, t.getX(), t.getY() + 0.5, t.getZ(), 30, 0.4, 0.5, 0.4, 0.1);
                }
            }
            case "gold_experience.c4" -> {
                if (ready(p, key, 400)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    for (LivingEntity e : Combat.around(p, Stands.range(p, 6))) {
                        if (e instanceof Player ally) {
                            effect(ally, MobEffects.REGENERATION, 100, 1);
                        } else if (e instanceof Enemy) {
                            hit(p, e, 8.0F);
                            effect(e, MobEffects.CONFUSION, 100, 0);
                        }
                    }
                    p.heal(4.0F);
                    lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 50, 3, 1, 3, 0.1);
                }
            }
            case "gold_experience.c5" -> {
                LivingEntity t = Combat.target(p, Stands.range(p, 8));
                if (t == null) { Combat.msg(p, "no_target"); return; }
                if (ready(p, key, 1200)) {
                    hit(p, t, 15.0F);
                    effect(t, MobEffects.MOVEMENT_SLOWDOWN, 160, 4);
                    effect(t, MobEffects.WEAKNESS, 160, 2);
                    effect(t, MobEffects.BLINDNESS, 160, 0);
                    ((ServerLevel) p.level()).sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + 1, t.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
                }
            }
            case "gold_experience.q1" -> {
                if (ready(p, key, 160)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    BlockPos c = p.blockPosition();
                    int grown = 0;
                    for (int dx = -6; dx <= 6; dx++) {
                        for (int dy = -2; dy <= 2; dy++) {
                            for (int dz = -6; dz <= 6; dz++) {
                                BlockPos pos = c.offset(dx, dy, dz);
                                if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), lvl, pos)) {
                                    grown++;
                                    lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
                                }
                            }
                        }
                    }
                    p.heal(6.0F);
                    effect(p, MobEffects.REGENERATION, 100, 1);
                    Combat.msg(p, "plants_grown", grown);
                }
            }
            case "gold_experience.q2" -> {
                if (ready(p, key, 300)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    List<EntityType<?>> animals = List.of(EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN, EntityType.RABBIT);
                    EntityType<?> type = animals.get(lvl.random.nextInt(animals.size()));
                    Entity e = type.create(lvl);
                    if (e != null) {
                        Vec3 pos = p.position().add(p.getLookAngle().multiply(2, 0, 2));
                        e.moveTo(pos.x, pos.y, pos.z, p.getYRot(), 0.0F);
                        lvl.addFreshEntity(e);
                        lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.5, pos.z, 20, 0.4, 0.5, 0.4, 0.1);
                    }
                }
            }

            // ===== MAGICIAN'S RED =====
            case "magicians_red.c1" -> {
                if (ready(p, key, 140)) cone(p, 10, 0.88, 160, 4.0F);
            }
            case "magicians_red.c2" -> {
                if (ready(p, key, 300)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    for (LivingEntity e : Combat.around(p, Stands.range(p, 6))) {
                        e.setRemainingFireTicks(100);
                        hit(p, e, 3.0F);
                    }
                    effect(p, MobEffects.FIRE_RESISTANCE, 200, 0);
                    for (int i = 0; i < 40; i++) {
                        double a = i * Math.PI * 2 / 40;
                        lvl.sendParticles(ParticleTypes.FLAME, p.getX() + Math.cos(a) * 5.5, p.getY() + 0.3, p.getZ() + Math.sin(a) * 5.5, 2, 0.1, 0.4, 0.1, 0.02);
                    }
                }
            }
            case "magicians_red.c3" -> {
                if (ready(p, key, 200)) {
                    double r = Stands.range(p, 14);
                    Combat.line(p, ParticleTypes.FLAME, r);
                    for (LivingEntity e : Combat.ray(p, r)) {
                        e.setRemainingFireTicks(120);
                        hit(p, e, 7.0F);
                    }
                }
            }
            case "magicians_red.c4" -> {
                if (ready(p, key, 500)) cone(p, 16, 0.8, 200, 10.0F);
            }
            case "magicians_red.c5" -> {
                if (ready(p, key, 1100)) {
                    ServerLevel lvl = (ServerLevel) p.level();
                    for (LivingEntity e : Combat.around(p, Stands.range(p, 9))) {
                        e.setRemainingFireTicks(240);
                        hit(p, e, 12.0F);
                        lvl.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY(), e.getZ(), 30, 0.3, 1.5, 0.3, 0.05);
                    }
                    lvl.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1, p.getZ(), 40, 4, 1, 4, 0.1);
                    sound(p, SoundEvents.BLAZE_SHOOT, 1.5F, 0.5F);
                }
            }
            case "magicians_red.q1" -> cook(p, key);
            case "magicians_red.q2" -> {
                if (ready(p, key, 300)) {
                    effect(p, MobEffects.FIRE_RESISTANCE, 3600, 0);
                    p.clearFire();
                }
            }
            default -> { }
        }
    }

    // ---------- habilidades mais longas ----------
    private static void cone(ServerPlayer p, double range, double dot, int fireTicks, float dmg) {
        ServerLevel lvl = (ServerLevel) p.level();
        double r = Stands.range(p, range);
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (double d = 1; d <= r; d += 0.7) {
            Vec3 pt = eye.add(look.scale(d));
            lvl.sendParticles(ParticleTypes.FLAME, pt.x, pt.y - 0.2, pt.z, 6, d * 0.06, d * 0.06, d * 0.06, 0.02);
        }
        for (LivingEntity e : Combat.cone(p, r, dot)) {
            e.setRemainingFireTicks(fireTicks);
            hit(p, e, dmg);
        }
    }

    private static void superVision(ServerPlayer p, String key) {
        if (!ready(p, key, 600)) return;
        effect(p, MobEffects.NIGHT_VISION, 2400, 0);
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32),
                x -> x instanceof Enemy && x.isAlive())) {
            effect(e, MobEffects.GLOWING, 200, 0);
        }
    }

    private static void grabItems(ServerPlayer p, String key) {
        if (!ready(p, key, 100)) return;
        int n = 0;
        for (ItemEntity i : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(16))) {
            i.setPos(p.getX(), p.getY() + 0.5, p.getZ());
            i.setPickUpDelay(0);
            n++;
        }
        Combat.msg(p, "items_grabbed", n);
    }

    private static void roadRoller(ServerPlayer p, String key) {
        if (!ready(p, key, 400)) return;
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.x * 0.5, 1.1, look.z * 0.5);
        p.hurtMarked = true;
        effect(p, MobEffects.SLOW_FALLING, 30, 0);
        Scheduler.later(10, () -> {
            if (!p.isAlive()) return;
            p.removeEffect(MobEffects.SLOW_FALLING);
            p.setDeltaMovement(0, -2.0, 0);
            p.hurtMarked = true;
        });
        Scheduler.later(16, () -> {
            if (!p.isAlive()) return;
            ServerLevel lvl = (ServerLevel) p.level();
            for (LivingEntity e : Combat.around(p, Stands.range(p, 5))) {
                hit(p, e, 12.0F);
                e.knockback(1.5, p.getX() - e.getX(), p.getZ() - e.getZ());
            }
            lvl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 6, 2, 0.2, 2, 0);
            lvl.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0F, 0.7F);
            p.fallDistance = 0;
        });
    }

    private static void blink(ServerPlayer p, String key) {
        Vec3 look = p.getLookAngle();
        double max = Stands.range(p, 12);
        for (double d = max; d >= 2; d -= 1.0) {
            Vec3 delta = look.scale(d);
            if (p.level().noCollision(p, p.getBoundingBox().move(delta))) {
                if (!ready(p, key, 100)) return;
                ServerLevel lvl = (ServerLevel) p.level();
                lvl.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.6, 0.3, 0.3);
                p.teleportTo(p.getX() + delta.x, p.getY() + delta.y, p.getZ() + delta.z);
                p.fallDistance = 0;
                lvl.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.6, 0.3, 0.3);
                return;
            }
        }
        Combat.msg(p, "no_space");
    }

    private static void killerBomb(ServerPlayer p, String key) {
        ServerLevel lvl = (ServerLevel) p.level();
        LivingEntity t = Combat.target(p, Stands.range(p, 6));
        if (t != null) {
            Set<UUID> set = BOMBS.computeIfAbsent(p.getUUID(), k -> new HashSet<>());
            if (set.size() >= 5) {
                Combat.msg(p, "max_bombs");
                return;
            }
            if (!ready(p, key, 30)) return;
            set.add(t.getUUID());
            effect(t, MobEffects.GLOWING, 1200, 0);
            lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, t.getX(), t.getY() + 1, t.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
            Combat.msg(p, "bomb_armed", set.size());
        } else {
            Set<UUID> set = BOMBS.get(p.getUUID());
            if (set == null || set.isEmpty()) {
                Combat.msg(p, "no_bombs");
                return;
            }
            if (!ready(p, key, 20)) return;
            BOMBS.remove(p.getUUID());
            for (UUID id : set) {
                Entity e = lvl.getEntity(id);
                if (e != null && e.isAlive()) {
                    boom(p, e.getX(), e.getY(0.5), e.getZ(), 3.2F, e instanceof LivingEntity le ? le : null, 8.0F);
                }
            }
            Combat.msg(p, "click");
        }
    }

    private static void blastTunnel(ServerPlayer p, String key) {
        if (!ready(p, key, 600)) return;
        ServerLevel lvl = (ServerLevel) p.level();
        Direction dir = p.getDirection();
        Direction side = dir.getClockWise();
        int broken = 0;
        for (int i = 1; i <= 6; i++) {
            BlockPos base = p.blockPosition().relative(dir, i);
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = 0; oy <= 2; oy++) {
                    BlockPos pos = base.relative(side, ox).above(oy);
                    BlockState st = lvl.getBlockState(pos);
                    if (st.isAir() || st.hasBlockEntity() || st.getDestroySpeed(lvl, pos) < 0) continue;
                    if (lvl.destroyBlock(pos, true, p)) broken++;
                }
            }
            lvl.sendParticles(ParticleTypes.EXPLOSION, base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
        sound(p, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8F, 1.0F);
        Combat.msg(p, "blocks_broken", broken);
    }

    private static final Map<Item, Item> COOKED = Map.of(
            Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP,
            Items.CHICKEN, Items.COOKED_CHICKEN, Items.MUTTON, Items.COOKED_MUTTON,
            Items.RABBIT, Items.COOKED_RABBIT, Items.COD, Items.COOKED_COD,
            Items.SALMON, Items.COOKED_SALMON, Items.POTATO, Items.BAKED_POTATO,
            Items.KELP, Items.DRIED_KELP);

    private static void cook(ServerPlayer p, String key) {
        if (!ready(p, key, 200)) return;
        int n = 0;
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            ItemStack s = p.getInventory().items.get(i);
            Item cooked = COOKED.get(s.getItem());
            if (cooked != null) {
                p.getInventory().items.set(i, new ItemStack(cooked, s.getCount()));
                n += s.getCount();
            }
        }
        Combat.msg(p, "food_cooked", n);
        sound(p, SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F);
    }

    // ---------- Bites the Dust ----------
    public static boolean isArmed(ServerPlayer p) {
        Long until = BTD_UNTIL.get(p.getUUID());
        return until != null && until > Scheduler.now();
    }

    /** Chamado a cada tick: guarda o estado dos ultimos ~10s enquanto Bites the Dust esta armado. */
    public static void historyTick(ServerPlayer p, int tick) {
        if (!BTD_UNTIL.containsKey(p.getUUID())) return;
        if (!isArmed(p)) {
            BTD_UNTIL.remove(p.getUUID());
            HISTORY.remove(p.getUUID());
            return;
        }
        if (tick % 20 != 0) return;
        Deque<Snap> q = HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        q.addLast(new Snap((ServerLevel) p.level(), p.getX(), p.getY(), p.getZ(), p.getHealth()));
        while (q.size() > 10) q.removeFirst();
    }

    /** Rebobina o tempo: volta ~10s no passado e revive. */
    public static void rewind(ServerPlayer p) {
        Deque<Snap> q = HISTORY.get(p.getUUID());
        Snap s = (q == null || q.isEmpty()) ? null : q.peekFirst();
        BTD_UNTIL.remove(p.getUUID());
        HISTORY.remove(p.getUUID());
        ServerLevel cur = (ServerLevel) p.level();
        cur.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.5);
        if (s != null) {
            p.teleportTo(s.level(), s.x(), s.y(), s.z(), p.getYRot(), p.getXRot());
            p.setHealth(Math.max(s.health(), 10.0F));
        } else {
            p.setHealth(Math.max(p.getHealth(), 10.0F));
        }
        for (MobEffectInstance i : new ArrayList<>(p.getActiveEffects())) {
            if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(i.getEffect());
        }
        p.clearFire();
        p.fallDistance = 0;
        p.invulnerableTime = 40;
        Combat.chat(p, "btd_rewind");
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5F, 0.5F);
        Awards.grant(p, "bites_the_dust");
    }
}
