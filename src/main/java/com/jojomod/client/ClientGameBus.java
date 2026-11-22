package com.jojomod.client;

import com.jojomod.AbilityList;
import com.jojomod.JojoMod;
import com.jojomod.network.ActionPayload;
import com.jojomod.network.ClientState;
import com.jojomod.network.SyncPayload;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = JojoMod.MOD_ID, value = Dist.CLIENT)
public class ClientGameBus {
    private static final ResourceLocation BLOOD_TEX =
            ResourceLocation.fromNamespaceAndPath(JojoMod.MOD_ID, "textures/gui/blood.png");

    // ---------- teclas ----------
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (ClientModBus.TOGGLE_STAND.consumeClick()) {
            PacketDistributor.sendToServer(new ActionPayload(0, ""));
        }
        while (ClientModBus.VAMPIRE.consumeClick()) {
            PacketDistributor.sendToServer(new ActionPayload(2, ""));
        }
        while (ClientModBus.USE_ABILITY.consumeClick()) {
            List<AbilityList.Entry> list = AbilityList.forState(ClientState.sync);
            if (!list.isEmpty()) {
                int idx = Math.floorMod(ClientState.wheelIndex, list.size());
                PacketDistributor.sendToServer(new ActionPayload(1, list.get(idx).id()));
            }
        }
        while (ClientModBus.STATUS.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new JojoStatusScreen());
        }
    }

    /** ALT + scroll troca a habilidade selecionada na roda. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !ClientModBus.WHEEL.isDown()) return;
        List<AbilityList.Entry> list = AbilityList.forState(ClientState.sync);
        if (list.isEmpty()) return;
        int dir = event.getScrollDeltaY() > 0 ? -1 : 1;
        ClientState.wheelIndex = Math.floorMod(ClientState.wheelIndex + dir, list.size());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }

    // ---------- tempo parado: inverte as cores do mundo ----------
    @SubscribeEvent
    public static void onGuiPre(RenderGuiEvent.Pre event) {
        if (!ClientState.timeStopped) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        g.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        b.addVertex(m, 0, 0, 0).setColor(255, 255, 255, 255);
        b.addVertex(m, 0, h, 0).setColor(255, 255, 255, 255);
        b.addVertex(m, w, h, 0).setColor(255, 255, 255, 255);
        b.addVertex(m, w, 0, 0).setColor(255, 255, 255, 255);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    // ---------- barra de sangue no lugar da fome ----------
    @SubscribeEvent
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())) return;
        SyncPayload s = ClientState.sync;
        Minecraft mc = Minecraft.getInstance();
        if (!s.vampire() || mc.player == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        event.setCanceled(true);

        GuiGraphics g = event.getGuiGraphics();
        int x0 = g.guiWidth() / 2 + 91 - 9;
        int y = g.guiHeight() - 39;
        for (int i = 0; i < 10; i++) {
            int val = s.blood() - i * 2;
            int u = val >= 2 ? 18 : val == 1 ? 9 : 0;
            g.blit(BLOOD_TEX, x0 - i * 8, y, (float) u, 0.0F, 9, 9, 27, 9);
        }
    }

    // ---------- roda de habilidades ----------
    @SubscribeEvent
    public static void onGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !ClientModBus.WHEEL.isDown()) return;
        renderWheel(event.getGuiGraphics(), mc.font);
    }

    private static void renderWheel(GuiGraphics g, Font font) {
        List<AbilityList.Entry> list = AbilityList.forState(ClientState.sync);
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.jojomod.wheel_empty"), cx, cy - 4, 0xAAAAAA);
            return;
        }
        int n = list.size();
        int sel = Math.floorMod(ClientState.wheelIndex, n);
        ClientState.wheelIndex = sel;
        int radius = 64;

        // anel de fundo
        for (int deg = 0; deg < 360; deg += 4) {
            double a = Math.toRadians(deg);
            int rx = cx + (int) (Math.cos(a) * radius);
            int ry = cy + (int) (Math.sin(a) * radius);
            g.fill(rx - 1, ry - 1, rx + 1, ry + 1, 0x55FFFFFF);
        }

        for (int i = 0; i < n; i++) {
            AbilityList.Entry e = list.get(i);
            double a = -Math.PI / 2 + 2 * Math.PI * i / n;
            int bx = cx + (int) (Math.cos(a) * radius) - 14;
            int by = cy + (int) (Math.sin(a) * radius) - 14;
            boolean selected = i == sel;

            int kindColor = switch (e.kind()) {
                case "combat" -> 0xFF8E44AD;
                case "qol" -> 0xFF27AE60;
                case "hamon" -> 0xFFF1C40F;
                default -> 0xFFC0392B;
            };
            int border = selected ? 0xFFFFFFFF : e.unlocked() ? kindColor : 0xFF444444;
            int grow = selected ? 3 : 1;
            g.fill(bx - grow, by - grow, bx + 28 + grow, by + 28 + grow, border);
            g.fill(bx, by, bx + 28, by + 28, e.unlocked() ? 0xEE15151F : 0xEE0A0A0A);
            g.drawCenteredString(font, String.valueOf(i + 1), bx + 14, by + 10, e.unlocked() ? 0xFFFFFF : 0x666666);
            if (!e.unlocked()) g.drawCenteredString(font, "X", bx + 14, by + 19, 0xFF5555);
        }

        AbilityList.Entry se = list.get(sel);
        g.drawCenteredString(font, Component.translatable("ability.jojomod." + se.id()), cx, cy - 12, se.unlocked() ? 0xFFFFFF : 0x888888);
        g.drawCenteredString(font, Component.translatable("ability.jojomod." + se.id() + ".desc"), cx, cy + 2, 0xAAAAAA);
        if (!se.unlocked()) {
            g.drawCenteredString(font, Component.translatable("gui.jojomod.locked"), cx, cy + 14, 0xFF5555);
        }
    }
}
