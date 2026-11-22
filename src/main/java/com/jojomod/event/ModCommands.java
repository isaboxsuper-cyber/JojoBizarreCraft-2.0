package com.jojomod.event;

import com.jojomod.JojoMod;
import com.jojomod.ModAttachments;
import com.jojomod.ModEntities;
import com.jojomod.Quests;
import com.jojomod.Stands;
import com.jojomod.data.JojoData;
import com.jojomod.entity.ZeppeliEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Comandos de teste (OP): /jojo stand <id> | hamon | vampire | xp <n> | zeppeli | reset */
@EventBusSubscriber(modid = JojoMod.MOD_ID)
public class ModCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jojo")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("stand")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Stands.IDS, b))
                                .executes(ctx -> setStand(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("hamon").executes(ctx -> setHamon(ctx.getSource())))
                .then(Commands.literal("vampire").executes(ctx -> setVampire(ctx.getSource())))
                .then(Commands.literal("xp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100000))
                                .executes(ctx -> addXp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.literal("zeppeli").executes(ctx -> zeppeli(ctx.getSource())))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource()))));
    }

    private static int setStand(CommandSourceStack src, String id) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!Stands.exists(id)) {
            src.sendFailure(Component.literal("Unknown stand: " + id));
            return 0;
        }
        JojoData d = p.getData(ModAttachments.JOJO);
        d.stand = id;
        d.standActive = false;
        d.standUnlocks = 0;
        d.stats = new ArrayList<>(List.of(0, 0, 0, 0, 0, 0));
        src.sendSuccess(() -> Component.literal("Stand: " + id), true);
        return 1;
    }

    private static int setHamon(CommandSourceStack src) throws CommandSyntaxException {
        JojoData d = src.getPlayerOrException().getData(ModAttachments.JOJO);
        d.vampire = false;
        d.hamon = true;
        d.hamonStage = Quests.QUESTS.length;
        d.hamonProgress = -1;
        d.hamonEnergy = 100;
        src.sendSuccess(() -> Component.literal("Hamon: all abilities unlocked"), true);
        return 1;
    }

    private static int setVampire(CommandSourceStack src) throws CommandSyntaxException {
        JojoData d = src.getPlayerOrException().getData(ModAttachments.JOJO);
        d.vampire = true;
        d.blood = 20;
        src.sendSuccess(() -> Component.literal("Vampire: on"), true);
        return 1;
    }

    private static int addXp(CommandSourceStack src, int amount) throws CommandSyntaxException {
        JojoData d = src.getPlayerOrException().getData(ModAttachments.JOJO);
        d.standXp += amount;
        src.sendSuccess(() -> Component.literal("Stand XP: " + d.standXp), true);
        return 1;
    }

    private static int zeppeli(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel lvl = (ServerLevel) p.level();
        ZeppeliEntity z = ModEntities.ZEPPELI.get().create(lvl);
        if (z == null) return 0;
        z.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0.0F);
        lvl.addFreshEntity(z);
        src.sendSuccess(() -> Component.literal("Zeppeli spawned"), true);
        return 1;
    }

    private static int reset(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        p.setData(ModAttachments.JOJO, new JojoData());
        src.sendSuccess(() -> Component.literal("JoJo data reset"), true);
        return 1;
    }
}
