package com.jojomod.client;

import com.jojomod.JojoMod;
import com.jojomod.Quests;
import com.jojomod.Stands;
import com.jojomod.network.ActionPayload;
import com.jojomod.network.ClientState;
import com.jojomod.network.SyncPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tela de status (tecla J): aba Info + aba Arvore de habilidades do Stand. */
public class JojoStatusScreen extends Screen {
    private static final int W = 340;
    private static final int H = 214;

    private int tab = 0;
    private int left;
    private int top;

    private record Node(String id, String slot, String nameKey, String descKey, int x, int y, int w, int h) {}

    public JojoStatusScreen() {
        super(Component.translatable("gui.jojomod.title"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.jojomod.tab_info"), b -> tab = 0)
                .bounds(left, top - 24, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.jojomod.tab_tree"), b -> tab = 1)
                .bounds(left + 104, top - 24, 120, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF000000);
        g.fill(left, top, left + W, top + H, 0xEE14141E);
        if (tab == 0) renderInfo(g);
        else renderTree(g, mx, my);
    }

    // =================== ABA INFO ===================
    private int line(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
        return y + 11;
    }

    private void renderInfo(GuiGraphics g) {
        SyncPayload s = ClientState.sync;
        int x = left + 8;
        int y = top + 8;

        // ---- Stand ----
        y = line(g, Component.translatable("gui.jojomod.section_stand"), x, y, 0xFFD700);
        if (s.stand().isEmpty()) {
            y = line(g, Component.translatable("gui.jojomod.no_stand"), x, y, 0x999999);
        } else {
            int level = 1 + Integer.bitCount(s.unlocks());
            for (int v : s.stats()) level += v;
            y = line(g, Component.translatable("stand.jojomod." + s.stand()), x, y, 0xFFFFFF);
            y = line(g, Component.translatable("gui.jojomod.level", level), x, y, 0xCCCCCC);
            y = line(g, Component.translatable("gui.jojomod.xp", s.xp()), x, y, 0x55FF55);
            y = line(g, Component.translatable(s.standActive() ? "gui.jojomod.stand_out" : "gui.jojomod.stand_in"), x, y, 0xAAAAAA);
            y = line(g, Component.translatable("gui.jojomod.passives"), x, y, 0xFFAA00);
            y = line(g, Component.translatable("ability.jojomod." + s.stand() + ".p1"), x + 4, y, 0xDDDDDD);
            if (Stands.unlocked("p2", s.unlocks())) {
                y = line(g, Component.translatable("ability.jojomod." + s.stand() + ".p2"), x + 4, y, 0xDDDDDD);
            } else {
                y = line(g, Component.translatable("gui.jojomod.p2_locked"), x + 4, y, 0x777777);
            }
            y = line(g, Component.translatable("gui.jojomod.stat_bonuses"), x, y, 0xFFAA00);
            y = line(g, Component.literal("+" + 8 * s.stats().get(0) + "% " + I18n.get("stat.jojomod.power")
                    + "  -" + 6 * s.stats().get(1) + "% CD"), x + 4, y, 0xDDDDDD);
            y = line(g, Component.literal("+" + (0.5 * s.stats().get(2)) + " " + I18n.get("stat.jojomod.range")
                    + "  -" + 2 * s.stats().get(3) + "% dmg"), x + 4, y, 0xDDDDDD);
        }

        // ---- Hamon ----
        y += 4;
        y = line(g, Component.translatable("gui.jojomod.section_hamon"), x, y, 0xFFD700);
        if (!s.hamon()) {
            y = line(g, Component.translatable("gui.jojomod.hamon_none"), x, y, 0x999999);
        } else {
            y = line(g, Component.translatable("gui.jojomod.hamon_stage", Math.min(s.hamonStage(), Quests.QUESTS.length), Quests.QUESTS.length), x, y, 0xFFFFFF);
            y = line(g, Component.translatable("gui.jojomod.hamon_energy", s.energy()), x, y, 0xFFDD55);
            y = line(g, Component.translatable("gui.jojomod.hamon_buff"), x, y, 0xDDDDDD);
        }

        // ---- Vampiro ----
        y += 4;
        y = line(g, Component.translatable("gui.jojomod.section_vampire"), x, y, 0xFFD700);
        if (!s.vampire()) {
            y = line(g, Component.translatable(s.maskOn() ? "gui.jojomod.vampire_ready" : "gui.jojomod.vampire_none"), x, y, 0x999999);
        } else {
            y = line(g, Component.translatable("gui.jojomod.blood", s.blood()), x, y, 0xFF5555);
            y = line(g, Component.translatable("gui.jojomod.vampire_buff"), x, y, 0xDDDDDD);
            y = line(g, Component.translatable(s.aja() ? "gui.jojomod.aja_yes" : "gui.jojomod.aja_no"), x, y, s.aja() ? 0xFF8844 : 0xAAAAAA);
            if (s.sunscreenSec() > 0) y = line(g, Component.translatable("gui.jojomod.sunscreen", s.sunscreenSec()), x, y, 0xFFFF55);
        }

        // ---- Hexagono ----
        if (!s.stand().isEmpty()) drawHexagon(g, left + W - 92, top + 108, 58, s);
    }

    private void drawHexagon(GuiGraphics g, int cx, int cy, int radius, SyncPayload s) {
        double[] px = new double[6];
        double[] py = new double[6];
        double[] sx = new double[6];
        double[] sy = new double[6];
        for (int i = 0; i < 6; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 3;
            px[i] = cx + Math.cos(a) * radius;
            py[i] = cy + Math.sin(a) * radius;
            double f = s.stats().get(i) / (double) Stands.MAX_STAT;
            f = Math.max(0.08, f);
            sx[i] = cx + Math.cos(a) * radius * f;
            sy[i] = cy + Math.sin(a) * radius * f;
        }
        // preenchimento da forma dos atributos (scanline, regra par-impar)
        for (int y = cy - radius; y <= cy + radius; y++) {
            List<Double> xs = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                int j = (i + 1) % 6;
                double y1 = sy[i];
                double y2 = sy[j];
                if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
                    xs.add(sx[i] + (y - y1) / (y2 - y1) * (sx[j] - sx[i]));
                }
            }
            xs.sort(Double::compare);
            for (int k = 0; k + 1 < xs.size(); k += 2) {
                g.fill((int) Math.round(xs.get(k)), y, (int) Math.round(xs.get(k + 1)), y + 1, 0xAA8E44AD);
            }
        }
        // contorno e eixos
        for (int ring = 1; ring <= Stands.MAX_STAT; ring++) {
            double f = ring / (double) Stands.MAX_STAT;
            for (int i = 0; i < 6; i++) {
                int j = (i + 1) % 6;
                drawLine(g, cx + (px[i] - cx) * f, cy + (py[i] - cy) * f, cx + (px[j] - cx) * f, cy + (py[j] - cy) * f,
                        ring == Stands.MAX_STAT ? 0xFFFFFFFF : 0x44FFFFFF);
            }
        }
        for (int i = 0; i < 6; i++) {
            drawLine(g, cx, cy, px[i], py[i], 0x66FFFFFF);
            Component name = Component.translatable("stat.jojomod." + Stands.STAT_KEYS[i]);
            int tw = font.width(name);
            double lx = cx + (px[i] - cx) * 1.28;
            double ly = cy + (py[i] - cy) * 1.2;
            g.drawString(font, name, (int) lx - tw / 2, (int) ly - 4, 0xFFFFFF, false);
            g.drawString(font, "Lv " + s.stats().get(i), (int) lx - 10, (int) ly + 6, 0xFFD700, false);
        }
    }

    private void drawLine(GuiGraphics g, double x1, double y1, double x2, double y2, int color) {
        int steps = (int) Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = (int) Math.round(x1 + (x2 - x1) * i / steps);
            int y = (int) Math.round(y1 + (y2 - y1) * i / steps);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    // =================== ABA ARVORE ===================
    private List<Node> buildNodes(SyncPayload s) {
        List<Node> nodes = new ArrayList<>();
        int bw = 60;
        int bh = 36;
        String st = s.stand();
        // combate
        String[] combat = {"c1", "c2", "c3", "c4", "c5"};
        for (int i = 0; i < combat.length; i++) {
            nodes.add(slotNode(st, combat[i], left + 10 + i * 64, top + 34, bw, bh));
        }
        // qualidade de vida + passivos
        nodes.add(slotNode(st, "q1", left + 10, top + 92, bw, bh));
        nodes.add(slotNode(st, "q2", left + 74, top + 92, bw, bh));
        nodes.add(slotNode(st, "p1", left + 202, top + 92, bw, bh));
        nodes.add(slotNode(st, "p2", left + 266, top + 92, bw, bh));
        // atributos
        for (int i = 0; i < 6; i++) {
            nodes.add(new Node("s" + i, "stat" + i, "stat.jojomod." + Stands.STAT_KEYS[i],
                    "stat.jojomod." + Stands.STAT_KEYS[i] + ".desc", left + 10 + i * 54, top + 152, 50, 40));
        }
        return nodes;
    }

    private Node slotNode(String stand, String slot, int x, int y, int w, int h) {
        int b = Stands.bit(slot);
        String id = b < 0 ? "free" : "u" + b;
        return new Node(id, slot, "ability.jojomod." + stand + "." + slot, "ability.jojomod." + stand + "." + slot + ".desc", x, y, w, h);
    }

    private boolean isUnlocked(SyncPayload s, Node n) {
        if (n.slot().startsWith("stat")) return false;
        return Stands.unlocked(n.slot(), s.unlocks());
    }

    private int costOf(SyncPayload s, Node n) {
        if (n.slot().startsWith("stat")) {
            int i = Integer.parseInt(n.slot().substring(4));
            int lvl = s.stats().get(i);
            return lvl >= Stands.MAX_STAT ? -1 : Stands.statCost(lvl);
        }
        return Stands.unlockCost(s.stand(), n.slot());
    }

    private void renderTree(GuiGraphics g, int mx, int my) {
        SyncPayload s = ClientState.sync;
        if (s.stand().isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.jojomod.no_stand"), left + W / 2, top + H / 2, 0x999999);
            return;
        }
        g.drawString(font, Component.translatable("gui.jojomod.xp", s.xp()), left + 10, top + 8, 0x55FF55, false);
        g.drawString(font, Component.translatable("gui.jojomod.tree_help"), left + 100, top + 8, 0x888888, false);
        g.drawString(font, Component.translatable("gui.jojomod.section_combat"), left + 10, top + 22, 0xFFAA00, false);
        g.drawString(font, Component.translatable("gui.jojomod.section_qol"), left + 10, top + 80, 0xFFAA00, false);
        g.drawString(font, Component.translatable("gui.jojomod.passives"), left + 202, top + 80, 0xFFAA00, false);
        g.drawString(font, Component.translatable("gui.jojomod.section_stats"), left + 10, top + 140, 0xFFAA00, false);

        Node hover = null;
        for (Node n : buildNodes(s)) {
            boolean stat = n.slot().startsWith("stat");
            boolean unlocked = isUnlocked(s, n);
            boolean free = n.id().equals("free");
            int cost = costOf(s, n);
            boolean afford = cost >= 0 && s.xp() >= cost;
            int color;
            if (stat) color = cost < 0 ? 0xFF27AE60 : afford ? 0xFFF1C40F : 0xFF555555;
            else if (unlocked || free) color = 0xFF27AE60;
            else color = afford ? 0xFFF1C40F : 0xFF555555;

            g.fill(n.x() - 1, n.y() - 1, n.x() + n.w() + 1, n.y() + n.h() + 1, color);
            g.fill(n.x(), n.y(), n.x() + n.w(), n.y() + n.h(), 0xFF1B1B26);
            String name = I18n.get(n.nameKey());
            g.drawString(font, font.plainSubstrByWidth(name, n.w() - 4), n.x() + 3, n.y() + 4, 0xFFFFFF, false);
            String sub;
            if (stat) sub = cost < 0 ? "MAX" : "Lv" + s.stats().get(Integer.parseInt(n.slot().substring(4))) + " | " + cost;
            else if (unlocked || free) sub = "OK";
            else sub = cost + " XP";
            g.drawString(font, sub, n.x() + 3, n.y() + n.h() - 12, afford || unlocked || free ? 0xAAFFAA : 0xFF8888, false);

            if (mx >= n.x() && mx < n.x() + n.w() && my >= n.y() && my < n.y() + n.h()) hover = n;
        }
        // setas simples entre os nos de combate
        for (int i = 0; i < 4; i++) {
            g.drawString(font, ">", left + 10 + i * 64 + 61, top + 48, 0xAAAAAA, false);
        }
        if (hover != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(hover.nameKey()));
            tip.add(Component.translatable(hover.descKey()));
            g.renderTooltip(font, tip.get(0), mx, my);
            g.drawString(font, Component.translatable(hover.descKey()), left + 10, top + H - 14, 0xDDDDDD, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (tab == 1 && button == 0 && !ClientState.sync.stand().isEmpty()) {
            for (Node n : buildNodes(ClientState.sync)) {
                if (mx >= n.x() && mx < n.x() + n.w() && my >= n.y() && my < n.y() + n.h()) {
                    if (!n.id().equals("free")) PacketDistributor.sendToServer(new ActionPayload(3, n.id()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
