package com.jojomod;

import com.jojomod.data.JojoData;
import com.jojomod.network.Sync;
import com.jojomod.util.Awards;
import com.jojomod.util.Combat;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;

/** Recebe as acoes do cliente (teclas / roda / arvore). */
public final class AbilityHandler {
    private AbilityHandler() {}

    public static void handle(ServerPlayer p, int type, String arg) {
        switch (type) {
            case 0 -> toggleStand(p);
            case 1 -> useAbility(p, arg);
            case 2 -> toggleVampire(p);
            case 3 -> Stands.buy(p, arg);
            default -> { }
        }
    }

    private static void toggleStand(ServerPlayer p) {
        JojoData d = p.getData(ModAttachments.JOJO);
        if (d.stand.isEmpty() || !Stands.exists(d.stand)) {
            Combat.msg(p, "no_stand");
            return;
        }
        d.standActive = !d.standActive;
        ServerLevel lvl = (ServerLevel) p.level();
        lvl.playSound(null, p.blockPosition(), d.standActive ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.8F, 1.5F);
        Combat.msg(p, d.standActive ? "stand_on" : "stand_off", net.minecraft.network.chat.Component.translatable("stand.jojomod." + d.stand));
    }

    private static void toggleVampire(ServerPlayer p) {
        JojoData d = p.getData(ModAttachments.JOJO);
        if (d.vampire) {
            d.vampire = false;
            Combat.msg(p, "vampire_off");
            return;
        }
        if (!p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STONE_MASK.get())) {
            Combat.msg(p, "need_mask");
            return;
        }
        if (p.getHealth() > 5.0F) p.hurt(p.damageSources().generic(), 4.0F);
        d.vampire = true;
        d.blood = 20;
        ServerLevel lvl = (ServerLevel) p.level();
        lvl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
        lvl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.5, p.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
        lvl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8F, 0.6F);
        Combat.chat(p, "vampire_on");
        Awards.grant(p, "phantom_blood");
    }

    private static void useAbility(ServerPlayer p, String id) {
        List<AbilityList.Entry> list = AbilityList.forState(Sync.snapshot(p));
        AbilityList.Entry entry = null;
        for (AbilityList.Entry e : list) if (e.id().equals(id)) entry = e;
        if (entry == null) {
            Combat.msg(p, "ability_unavailable");
            return;
        }
        if (!entry.unlocked()) {
            Combat.msg(p, "ability_locked");
            return;
        }
        if (id.startsWith("hamon.")) {
            HamonAbilities.run(p, id.substring(6));
        } else if (id.startsWith("vampire.")) {
            VampireAbilities.run(p, id.substring(8));
        } else {
            int dot = id.indexOf('.');
            StandAbilities.run(p, id.substring(0, dot), id.substring(dot + 1));
        }
    }
}
