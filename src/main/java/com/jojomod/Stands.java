package com.jojomod;

import com.jojomod.data.JojoData;
import com.jojomod.util.Awards;
import com.jojomod.util.Combat;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Dados dos Stands, desbloqueios, atributos (hexagono) e XP. */
public final class Stands {
    public static final List<String> IDS = List.of(
            "star_platinum", "the_world", "crazy_diamond", "killer_queen", "gold_experience", "magicians_red");
    private static final int[] WEIGHTS = {10, 5, 10, 8, 8, 10};

    /** Habilidades selecionaveis na roda: 5 de combate + 2 de qualidade de vida. */
    public static final String[] SLOTS = {"c1", "c2", "c3", "c4", "c5", "q1", "q2"};
    /** Cada indice = 1 bit em JojoData.standUnlocks. */
    public static final String[] UNLOCK_SLOTS = {"c2", "c3", "c4", "c5", "q1", "q2", "p2"};
    public static final String[] STAT_KEYS = {"power", "speed", "range", "durability", "precision", "potential"};
    public static final int MAX_STAT = 5;

    private Stands() {}

    public static boolean exists(String id) {
        return IDS.contains(id);
    }

    public static String random(RandomSource r) {
        int total = 0;
        for (int w : WEIGHTS) total += w;
        int roll = r.nextInt(total);
        for (int i = 0; i < IDS.size(); i++) {
            roll -= WEIGHTS[i];
            if (roll < 0) return IDS.get(i);
        }
        return IDS.get(0);
    }

    public static int bit(String slot) {
        for (int i = 0; i < UNLOCK_SLOTS.length; i++) if (UNLOCK_SLOTS[i].equals(slot)) return i;
        return -1;
    }

    /** c1 e p1 sao gratuitas. */
    public static boolean unlocked(String slot, int unlocks) {
        int b = bit(slot);
        return b < 0 || (unlocks & (1 << b)) != 0;
    }

    public static int unlockCost(String stand, String slot) {
        return switch (slot) {
            case "c2" -> 100;
            case "c3" -> 250;
            case "c4" -> 500;
            case "c5" -> stand.equals("killer_queen") ? 2000 : 1000; // Bites the Dust e a mais dificil
            case "q1" -> 150;
            case "q2" -> 400;
            case "p2" -> 300;
            default -> 0;
        };
    }

    public static int statCost(int currentLevel) {
        return 60 * (currentLevel + 1);
    }

    // ---------- bonus dos atributos (somente servidor) ----------
    private static JojoData data(ServerPlayer p) {
        return p.getData(ModAttachments.JOJO);
    }

    /** Poder: +8% de dano por nivel. */
    public static float dmg(ServerPlayer p, float base) {
        return base * (1.0F + 0.08F * data(p).stat(0));
    }

    /** Velocidade: -6% de recarga por nivel. */
    public static int cd(ServerPlayer p, int ticks) {
        return Math.max(10, (int) (ticks * (1.0 - 0.06 * data(p).stat(1))));
    }

    /** Alcance: +0.5 bloco por nivel. */
    public static double range(ServerPlayer p, double base) {
        return base + 0.5 * data(p).stat(2);
    }

    /** Chance de passivos: sobe com o nivel do Stand e com a Precisao. */
    public static double passiveChance(ServerPlayer p, double base) {
        JojoData d = data(p);
        return Math.min(0.95, base + 0.02 * (d.level() - 1) + 0.03 * d.stat(4));
    }

    /** Durabilidade: -2% de dano recebido por nivel. */
    public static float durabilityFactor(ServerPlayer p) {
        return 1.0F - 0.02F * data(p).stat(3);
    }

    /** Potencial: +10% de XP por nivel. */
    public static void addXp(ServerPlayer p, int amount) {
        JojoData d = data(p);
        if (d.stand.isEmpty()) return;
        d.standXp += (int) Math.ceil(amount * (1.0 + 0.1 * d.stat(5)));
    }

    // ---------- arvore de habilidades ----------
    public static void buy(ServerPlayer p, String node) {
        JojoData d = data(p);
        if (d.stand.isEmpty() || node == null || node.length() < 2) return;
        int idx;
        try {
            idx = Integer.parseInt(node.substring(1));
        } catch (NumberFormatException e) {
            return;
        }

        if (node.charAt(0) == 'u') {
            if (idx < 0 || idx >= UNLOCK_SLOTS.length) return;
            if ((d.standUnlocks & (1 << idx)) != 0) {
                Combat.msg(p, "already_unlocked");
                return;
            }
            if (idx >= 1 && idx <= 3 && (d.standUnlocks & (1 << (idx - 1))) == 0) {
                Combat.msg(p, "need_previous");
                return;
            }
            String slot = UNLOCK_SLOTS[idx];
            int cost = unlockCost(d.stand, slot);
            if (d.standXp < cost) {
                Combat.msg(p, "not_enough_xp", cost);
                return;
            }
            d.standXp -= cost;
            d.standUnlocks |= (1 << idx);
            Combat.msg(p, "unlocked", Component.translatable("ability.jojomod." + d.stand + "." + slot));
            if (slot.equals("c5")) Awards.grant(p, "diamond_is_unbreakable");
        } else if (node.charAt(0) == 's') {
            if (idx < 0 || idx >= STAT_KEYS.length) return;
            int lvl = d.stat(idx);
            if (lvl >= MAX_STAT) {
                Combat.msg(p, "stat_max");
                return;
            }
            int cost = statCost(lvl);
            if (d.standXp < cost) {
                Combat.msg(p, "not_enough_xp", cost);
                return;
            }
            d.standXp -= cost;
            d.setStat(idx, lvl + 1);
            Combat.msg(p, "stat_up", Component.translatable("stat.jojomod." + STAT_KEYS[idx]), lvl + 1);
        }
    }
}
