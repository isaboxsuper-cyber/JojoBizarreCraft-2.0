package com.jojomod.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/** Todos os dados JoJo de um jogador (salvo no jogador). */
public class JojoData {
    public String stand = "";
    public boolean standActive = false;
    public int standXp = 0;
    public int standUnlocks = 0;
    public List<Integer> stats = new ArrayList<>(List.of(0, 0, 0, 0, 0, 0));
    public boolean hamon = false;
    public int hamonStage = 0;
    public int hamonProgress = -1;
    public int hamonEnergy = 0;
    public boolean vampire = false;
    public boolean aja = false;
    public int blood = 20;
    public int sunscreen = 0;

    public JojoData() {}

    public JojoData(String stand, boolean standActive, int standXp, int standUnlocks, List<Integer> stats,
                    boolean hamon, int hamonStage, int hamonProgress, int hamonEnergy,
                    boolean vampire, boolean aja, int blood, int sunscreen) {
        this.stand = stand;
        this.standActive = standActive;
        this.standXp = standXp;
        this.standUnlocks = standUnlocks;
        this.stats = new ArrayList<>(stats);
        while (this.stats.size() < 6) this.stats.add(0);
        this.hamon = hamon;
        this.hamonStage = hamonStage;
        this.hamonProgress = hamonProgress;
        this.hamonEnergy = hamonEnergy;
        this.vampire = vampire;
        this.aja = aja;
        this.blood = blood;
        this.sunscreen = sunscreen;
    }

    public static final Codec<JojoData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("stand", "").forGetter(d -> d.stand),
            Codec.BOOL.optionalFieldOf("standActive", false).forGetter(d -> d.standActive),
            Codec.INT.optionalFieldOf("standXp", 0).forGetter(d -> d.standXp),
            Codec.INT.optionalFieldOf("standUnlocks", 0).forGetter(d -> d.standUnlocks),
            Codec.INT.listOf().optionalFieldOf("stats", List.of(0, 0, 0, 0, 0, 0)).forGetter(d -> d.stats),
            Codec.BOOL.optionalFieldOf("hamon", false).forGetter(d -> d.hamon),
            Codec.INT.optionalFieldOf("hamonStage", 0).forGetter(d -> d.hamonStage),
            Codec.INT.optionalFieldOf("hamonProgress", -1).forGetter(d -> d.hamonProgress),
            Codec.INT.optionalFieldOf("hamonEnergy", 0).forGetter(d -> d.hamonEnergy),
            Codec.BOOL.optionalFieldOf("vampire", false).forGetter(d -> d.vampire),
            Codec.BOOL.optionalFieldOf("aja", false).forGetter(d -> d.aja),
            Codec.INT.optionalFieldOf("blood", 20).forGetter(d -> d.blood),
            Codec.INT.optionalFieldOf("sunscreen", 0).forGetter(d -> d.sunscreen)
    ).apply(i, JojoData::new));

    public JojoData copy() {
        return new JojoData(stand, standActive, standXp, standUnlocks, stats, hamon, hamonStage,
                hamonProgress, hamonEnergy, vampire, aja, blood, sunscreen);
    }

    public int stat(int i) {
        return i >= 0 && i < stats.size() ? stats.get(i) : 0;
    }

    public void setStat(int i, int v) {
        while (stats.size() < 6) stats.add(0);
        stats.set(i, v);
    }

    /** Nivel do Stand = 1 + habilidades desbloqueadas + niveis de atributos. */
    public int level() {
        int l = 1 + Integer.bitCount(standUnlocks);
        for (int s : stats) l += s;
        return l;
    }
}
