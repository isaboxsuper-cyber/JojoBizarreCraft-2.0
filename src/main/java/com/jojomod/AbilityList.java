package com.jojomod;

import com.jojomod.network.SyncPayload;
import java.util.ArrayList;
import java.util.List;

/** Lista de habilidades da Roda (cliente e servidor usam a mesma logica). */
public final class AbilityList {
    public record Entry(String id, boolean unlocked, String kind) {}

    public static final String[] HAMON = {"overdrive", "zoom_punch", "turquoise", "sendo_kick", "water_walk"};
    public static final String[] VAMPIRE = {"drain", "eyes", "freeze", "flesh_bud"};

    private AbilityList() {}

    public static List<Entry> forState(SyncPayload s) {
        List<Entry> out = new ArrayList<>();
        if (s.standActive() && Stands.exists(s.stand())) {
            for (String slot : Stands.SLOTS) {
                out.add(new Entry(s.stand() + "." + slot, Stands.unlocked(slot, s.unlocks()),
                        slot.startsWith("c") ? "combat" : "qol"));
            }
        } else if (s.vampire()) {
            for (String v : VAMPIRE) out.add(new Entry("vampire." + v, true, "vampire"));
        } else if (s.hamon()) {
            for (int i = 0; i < HAMON.length; i++) {
                out.add(new Entry("hamon." + HAMON[i], s.hamonStage() > i, "hamon"));
            }
        }
        return out;
    }
}
