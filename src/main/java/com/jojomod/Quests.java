package com.jojomod;

import com.jojomod.data.JojoData;
import com.jojomod.entity.ZeppeliEntity;
import com.jojomod.util.Awards;
import com.jojomod.util.Combat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Missoes do Zeppeli: cada missao concluida desbloqueia uma habilidade do Hamon. */
public final class Quests {
    public record Quest(String type, int target, String item) {}

    public static final Quest[] QUESTS = {
            new Quest("kill_undead", 10, ""),                 // 0 -> Hamon + Sunlight Yellow Overdrive
            new Quest("breath", 1800, ""),                    // 1 -> Zoom Punch (90s respirando)
            new Quest("deliver", 8, "minecraft:sunflower"),   // 2 -> Turquoise Blue Overdrive
            new Quest("kill_undead", 30, ""),                 // 3 -> Sendo Wave Kick
            new Quest("water", 1200, "")                      // 4 -> Hamon sobre a agua (60s na agua)
    };

    private Quests() {}

    private static Item itemOf(Quest q) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.item()));
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static void remove(ServerPlayer p, Item item, int amount) {
        int left = amount;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (s.is(item)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
    }

    private static int shown(Quest q, int value) {
        return q.type().equals("breath") || q.type().equals("water") ? value / 20 : value;
    }

    public static void talk(ServerPlayer p, ZeppeliEntity z) {
        JojoData d = p.getData(ModAttachments.JOJO);
        if (d.vampire) {
            Combat.chat(p, "zeppeli_vampire");
            return;
        }
        if (d.hamonStage >= QUESTS.length) {
            Combat.chat(p, "zeppeli_done");
            return;
        }
        int n = d.hamonStage;
        Quest q = QUESTS[n];

        if (d.hamonProgress < 0) {
            d.hamonProgress = 0;
            Combat.chat(p, "zeppeli_new_quest", Component.translatable("quest.jojomod." + n + ".title"));
            Combat.chat(p, "quest_desc", Component.translatable("quest.jojomod." + n + ".desc"));
            return;
        }

        boolean done;
        int current;
        if (q.type().equals("deliver")) {
            current = count(p, itemOf(q));
            done = current >= q.target();
        } else {
            current = d.hamonProgress;
            done = current >= q.target();
        }

        if (!done) {
            Combat.chat(p, "zeppeli_not_yet");
            Combat.chat(p, "quest_progress", shown(q, current), shown(q, q.target()));
            return;
        }

        if (q.type().equals("deliver")) remove(p, itemOf(q), q.target());
        complete(p, d, z);
    }

    private static void complete(ServerPlayer p, JojoData d, ZeppeliEntity z) {
        int finished = d.hamonStage;
        d.hamonStage++;
        d.hamonProgress = -1;
        if (finished == 0) {
            d.hamon = true;
            d.hamonEnergy = 100;
            Awards.grant(p, "battle_tendency");
        }
        if (d.hamonStage >= QUESTS.length) Awards.grant(p, "steel_ball_run");

        p.level().playSound(null, z.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0F, 1.2F);
        Combat.chat(p, "quest_complete", Component.translatable("quest.jojomod." + finished + ".title"));
        Combat.chat(p, "quest_unlock", Component.translatable("ability.jojomod.hamon." + AbilityList.HAMON[finished]));
        if (d.hamonStage < QUESTS.length) Combat.chat(p, "zeppeli_next");
    }

    /** Chamado quando o jogador mata um morto-vivo / vampiro. */
    public static void onKill(ServerPlayer p) {
        JojoData d = p.getData(ModAttachments.JOJO);
        if (d.vampire || d.hamonProgress < 0 || d.hamonStage >= QUESTS.length) return;
        Quest q = QUESTS[d.hamonStage];
        if (!q.type().equals("kill_undead")) return;
        d.hamonProgress++;
        if (d.hamonProgress == q.target()) Combat.msg(p, "quest_return");
    }

    /** Chamado todo tick: respiracao (Shift) e tempo na agua. */
    public static void tick(ServerPlayer p, JojoData d) {
        if (d.vampire || d.hamonProgress < 0 || d.hamonStage >= QUESTS.length) return;
        Quest q = QUESTS[d.hamonStage];
        boolean count = switch (q.type()) {
            case "breath" -> p.isShiftKeyDown();
            case "water" -> p.isInWater();
            default -> false;
        };
        if (count && d.hamonProgress < q.target()) {
            d.hamonProgress++;
            if (d.hamonProgress == q.target()) Combat.msg(p, "quest_return");
        }
    }
}
