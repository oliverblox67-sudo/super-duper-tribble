package com.thegods.client.gui;

import com.thegods.god.God;
import com.thegods.god.GodData;
import com.thegods.god.Limb;
import com.thegods.god.LimbManager;
import com.thegods.god.LimbState;
import com.thegods.network.LimbActionPayload;
import com.thegods.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * FORGIA DEL CORPO
 * A sinistra l'anteprima 3D dal vivo, al centro lo schema del corpo (clicca un arto),
 * a destra stato, effetti e le azioni: Recidi / Forgia d'Acqua / Rigenera.
 */
public class BodyForgeScreen extends Screen {
    private static final int W = 340, H = 206;
    private Limb selected = Limb.RIGHT_ARM;
    private Button sever, forge, restore;
    private int left, top;
    private float time;

    public BodyForgeScreen() {
        super(Component.translatable("screen.thegods.body_forge"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bx = left + 210, bw = 120;
        sever = addRenderableWidget(Button.builder(Component.translatable("screen.thegods.body_forge.sever"),
                b -> send(LimbManager.Action.SEVER)).bounds(bx, top + 128, bw, 20).build());
        forge = addRenderableWidget(Button.builder(Component.translatable("screen.thegods.body_forge.forge", (int) LimbManager.FORGE_COST),
                b -> send(LimbManager.Action.FORGE)).bounds(bx, top + 151, bw, 20).build());
        restore = addRenderableWidget(Button.builder(Component.translatable("screen.thegods.body_forge.restore", (int) LimbManager.RESTORE_COST),
                b -> send(LimbManager.Action.RESTORE)).bounds(bx, top + 174, bw, 20).build());
        updateButtons();
    }

    private GodData data() {
        return minecraft.player.getData(ModAttachments.GOD_DATA);
    }

    private void send(LimbManager.Action action) {
        PacketDistributor.sendToServer(new LimbActionPayload(selected.ordinal(), action.ordinal()));
    }

    private void updateButtons() {
        GodData d = data();
        boolean god = d.hasGod();
        LimbState st = d.limb(selected);
        sever.active = god && st != LimbState.SEVERED;
        forge.active = god && st != LimbState.CONSTRUCT;
        restore.active = god && st != LimbState.FLESH;
    }

    @Override
    public void tick() {
        updateButtons();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.renderBackground(g, mouseX, mouseY, pt);
        // pannello
        g.fillGradient(left, top, left + W, top + H, 0xF0061426, 0xF0020812);
        g.fill(left, top, left + W, top + 1, 0xFF2FB8FF);
        g.fill(left, top + H - 1, left + W, top + H, 0xFF1560C8);
        g.fill(left, top, left + 1, top + H, 0xFF1560C8);
        g.fill(left + W - 1, top, left + W, top + H, 0xFF1560C8);
        // riquadro anteprima
        g.fill(left + 8, top + 24, left + 98, top + H - 8, 0x60000000);
        g.fill(left + 108, top + 24, left + 200, top + H - 8, 0x40000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        time += pt;
        GodData d = data();

        Component title = getTitle().copy().withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 8, 0xFFBDF4FF, true);

        // anteprima 3D dal vivo
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 8, top + 24, left + 98, top + H - 8, 50, 0.0625f,
                mouseX, mouseY, minecraft.player);

        // schema del corpo
        drawBody(g, d, mouseX, mouseY);

        // dettagli arto
        int x = left + 210, y = top + 28;
        g.drawString(font, Component.translatable(selected.nameKey()).withStyle(ChatFormatting.BOLD), x, y, 0xFFFFFFFF, true);
        LimbState st = d.limb(selected);
        int stColor = switch (st) {
            case FLESH -> 0xFFE8C9A8;
            case SEVERED -> 0xFFFF6B6B;
            case CONSTRUCT -> 0xFF5FD8FF;
        };
        g.drawString(font, Component.translatable(st.nameKey()), x, y + 12, stColor, true);
        int ly = y + 28;
        for (Component line : effects(selected, st)) {
            for (FormattedCharSequence seq : font.split(line, 122)) {
                g.drawString(font, seq, x, ly, 0xFFB0C8D6, false);
                ly += 10;
            }
        }
        if (!selected.isArm() && d.count(LimbState.CONSTRUCT, false) == 2) {
            g.drawString(font, Component.translatable("limb.thegods.effect.legs.both"), x, ly + 2, 0xFF7FE8FF, true);
        }

        God god = d.god();
        Component footer = god == null ? Component.translatable("screen.thegods.body_forge.need_god").withStyle(ChatFormatting.RED)
                : Component.translatable("screen.thegods.body_forge.energy", (int) d.energy());
        g.drawString(font, footer, left + 108 + (92 - font.width(footer)) / 2, top + H - 20, 0xFF9FE6FF, false);
    }

    private List<Component> effects(Limb limb, LimbState st) {
        List<Component> out = new ArrayList<>();
        String kind = limb.isArm() ? "arm" : "leg";
        switch (st) {
            case FLESH -> out.add(Component.translatable("limb.thegods.effect.flesh"));
            case SEVERED -> out.add(Component.translatable("limb.thegods.effect." + kind + ".severed"));
            case CONSTRUCT -> {
                out.add(Component.translatable("limb.thegods.effect." + kind + ".construct.1"));
                out.add(Component.translatable("limb.thegods.effect." + kind + ".construct.2"));
            }
        }
        return out;
    }

    // --------------------------------------------------------------- schema del corpo

    /** Rettangoli dello schema: x, y, w, h relativi al centro dello schema. */
    private int[] rect(Limb limb) {
        int cx = left + 154, ty = top + 40;
        return switch (limb) { // vista frontale: il braccio destro è a sinistra dello schermo
            case RIGHT_ARM -> new int[]{cx - 27, ty + 26, 12, 42};
            case LEFT_ARM -> new int[]{cx + 15, ty + 26, 12, 42};
            case RIGHT_LEG -> new int[]{cx - 13, ty + 70, 12, 50};
            case LEFT_LEG -> new int[]{cx + 1, ty + 70, 12, 50};
        };
    }

    private void drawBody(GuiGraphics g, GodData d, int mx, int my) {
        int cx = left + 154, ty = top + 40;
        // testa e busto (non selezionabili)
        g.fill(cx - 11, ty, cx + 11, ty + 22, 0xFFC69C6D);
        g.fill(cx - 13, ty + 26, cx + 13, ty + 68, 0xFF3D7FB0);
        outline(g, cx - 11, ty, 22, 22, 0xFF6B4A2E);
        outline(g, cx - 13, ty + 26, 26, 42, 0xFF1E3E58);

        for (Limb limb : Limb.ALL) {
            int[] r = rect(limb);
            LimbState st = d.limb(limb);
            boolean hover = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
            switch (st) {
                case FLESH -> {
                    g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], limb.isArm() ? 0xFFC69C6D : 0xFF2E3A6B);
                    outline(g, r[0], r[1], r[2], r[3], 0xFF3A2A1A);
                }
                case SEVERED -> {
                    for (int yy = r[1]; yy < r[1] + r[3]; yy += 4) {
                        g.fill(r[0], yy, r[0] + 2, yy + 2, 0xFFFF5555);
                        g.fill(r[0] + r[2] - 2, yy, r[0] + r[2], yy + 2, 0xFFFF5555);
                    }
                    for (int xx = r[0]; xx < r[0] + r[2]; xx += 4) {
                        g.fill(xx, r[1], xx + 2, r[1] + 2, 0xFFFF5555);
                        g.fill(xx, r[1] + r[3] - 2, xx + 2, r[1] + r[3], 0xFFFF5555);
                    }
                    g.drawString(font, "✂", r[0] + r[2] / 2 - 3, r[1] + r[3] / 2 - 4, 0xFFFF8888, false);
                }
                case CONSTRUCT -> {
                    for (int yy = 0; yy < r[3]; yy++) {
                        float w = 0.5f + 0.5f * Mth.sin(yy * 0.35f - time * 0.25f);
                        int c = 0xE0000000 | lerp(w, 0x1560C8, 0x8FE8FF);
                        g.fill(r[0], r[1] + yy, r[0] + r[2], r[1] + yy + 1, c);
                    }
                    outline(g, r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, 0xFFBFF6FF);
                }
            }
            if (limb == selected) {
                int a = (int) (160 + 95 * Mth.sin(time * 0.3f));
                outline(g, r[0] - 3, r[1] - 3, r[2] + 6, r[3] + 6, (a << 24) | 0xFFFFFF);
            } else if (hover) {
                outline(g, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, 0x90FFFFFF);
            }
        }
        g.drawString(font, Component.translatable("screen.thegods.body_forge.right"), cx - 36, ty + 124, 0xFF7F95A3, false);
        g.drawString(font, Component.translatable("screen.thegods.body_forge.left"), cx + 16, ty + 124, 0xFF7F95A3, false);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private static int lerp(float t, int a, int b) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int gg = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (gg << 8) | bl;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Limb limb : Limb.ALL) {
            int[] r = rect(limb);
            if (mx >= r[0] - 2 && mx < r[0] + r[2] + 2 && my >= r[1] - 2 && my < r[1] + r[3] + 2) {
                selected = limb;
                updateButtons();
                minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, 1.4f);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Usato dalla demo automatica. */
    public void select(Limb limb) {
        this.selected = limb;
    }
}
