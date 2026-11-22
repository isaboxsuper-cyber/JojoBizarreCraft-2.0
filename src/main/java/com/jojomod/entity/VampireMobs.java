package com.jojomod.entity;

import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/** Mobs transformados em vampiros (pela Mascara de Pedra) ou ghouls (Flesh Bud). */
public final class VampireMobs {
    public static final String TAG = "jojomod_vampire";
    public static final String GHOUL = "jojomod_ghoul";

    private VampireMobs() {}

    public static boolean is(Entity e) {
        return e instanceof Mob m && m.getPersistentData().getBoolean(TAG);
    }

    public static void convert(PathfinderMob mob, boolean ghoul) {
        CompoundTag tag = mob.getPersistentData();
        tag.putBoolean(TAG, true);
        if (ghoul) tag.putBoolean(GHOUL, true);
        mob.setPersistenceRequired();
        applyAi(mob);
        if (ghoul) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 1, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
    }

    /** Remove os alvos normais (nao ataca jogadores) e adiciona o ataque a mobs nao-vampiros. */
    public static void applyAi(PathfinderMob mob) {
        mob.setTarget(null);
        GoalSelector targets = mob.targetSelector;
        for (WrappedGoal w : new ArrayList<>(targets.getAvailableGoals())) {
            targets.removeGoal(w.getGoal());
        }
        boolean has = false;
        for (WrappedGoal w : mob.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof VampireAttackGoal) has = true;
        }
        if (!has) mob.goalSelector.addGoal(1, new VampireAttackGoal(mob));
    }
}
