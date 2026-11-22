package com.jojomod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue ARROW_DEATH_CHANCE = B
            .comment("Chance (0.0 a 1.0) da Flecha de Stand matar quem a usa. / Chance the Stand Arrow kills the user.")
            .defineInRange("arrowDeathChance", 0.10D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue VAMPIRES_BURN = B
            .comment("Vampiros queimam ao sol. / Vampires burn in sunlight.")
            .define("vampiresBurnInSunlight", true);

    public static final ModConfigSpec SPEC = B.build();
}
