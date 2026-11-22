package com.jojomod;

import com.jojomod.entity.ZeppeliEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, JojoMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ZeppeliEntity>> ZEPPELI = ENTITIES.register("zeppeli",
            () -> EntityType.Builder.of(ZeppeliEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("zeppeli"));
}
