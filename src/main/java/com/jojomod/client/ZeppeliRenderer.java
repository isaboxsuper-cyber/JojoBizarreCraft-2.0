package com.jojomod.client;

import com.jojomod.JojoMod;
import com.jojomod.entity.ZeppeliEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ZeppeliRenderer extends HumanoidMobRenderer<ZeppeliEntity, HumanoidModel<ZeppeliEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JojoMod.MOD_ID, "textures/entity/zeppeli.png");

    public ZeppeliRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ZeppeliEntity entity) {
        return TEXTURE;
    }
}
