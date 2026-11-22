package com.jojomod.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

/** Mobs vampiros/ghouls atacam qualquer mob que nao seja vampiro. */
public class VampireAttackGoal extends Goal {
    private final PathfinderMob mob;
    private LivingEntity target;
    private int cooldown = 0;
    private int repath = 0;

    public VampireAttackGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean valid(Mob e) {
        return e != mob && e.isAlive() && !VampireMobs.is(e) && !(e instanceof ZeppeliEntity)
                && !(e instanceof TamableAnimal t && t.isTame());
    }

    @Override
    public boolean canUse() {
        if (mob.getRandom().nextInt(10) != 0) return false;
        List<Mob> list = mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(14), this::valid);
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob m : list) {
            double d = m.distanceToSqr(mob);
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        target = best;
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && mob.distanceToSqr(target) < 24 * 24;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--repath <= 0) {
            repath = 8;
            mob.getNavigation().moveTo(target, 1.15D);
        }
        if (cooldown > 0) cooldown--;
        double reach = mob.getBbWidth() * 2.0D;
        if (cooldown == 0 && mob.distanceToSqr(target) <= reach * reach + target.getBbWidth()) {
            mob.swing(InteractionHand.MAIN_HAND);
            target.hurt(mob.damageSources().mobAttack(mob), 5.0F);
            cooldown = 20;
        }
    }
}
