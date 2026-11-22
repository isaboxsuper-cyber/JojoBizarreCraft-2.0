package com.jojomod.util;

import com.jojomod.JojoMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Concede as conquistas (advancements) do mod. */
public final class Awards {
    private Awards() {}

    public static void grant(ServerPlayer p, String id) {
        AdvancementHolder holder = p.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(JojoMod.MOD_ID, id));
        if (holder == null) return;
        AdvancementProgress progress = p.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            p.getAdvancements().award(holder, criterion);
        }
    }
}
