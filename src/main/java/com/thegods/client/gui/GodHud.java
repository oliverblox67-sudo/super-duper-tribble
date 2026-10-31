package com.thegods.client.gui;

import com.thegods.client.ClientGodState;
import com.thegods.client.ModKeys;
import com.thegods.config.GodsConfig;
import com.thegods.god.Ability;
import com.thegods.god.God;
import com.thegods.god.GodData;
import com.thegods.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Pannello laterale: nome del dio, energia divina e lista dei poteri con ricariche. */
public final class GodHud {
    private static final int WIDTH = 168;
    private static final int ROW = 12;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        GodData data = mc.player.getData(ModAttachments.GOD_DATA);
        God god = data.god();
        if (god == null) return;

        Font font = mc.font;
        List<Ability> abilities = god.activeAbilities(data);
        boolean avatar = data.isAvatar();
        int col = god.color() & 0xFFFFFF;
        int height = 26 + abilities.size() * ROW + 12;
        int x = 6;
        int y = g.guiHeight() / 2 - height / 2;
        boolean active = data.isManifested();

        // pannello
        g.fillGradient(x - 3, y - 3, x + WIDTH, y + height, 0x70000814, 0x40000814);
        g.fill(x - 3, y - 3, x - 2, y + height, 0xFF000000 | col);

        // nome + energia
        Component name = (avatar ? Component.translatable("hud.thegods.avatar") : god.displayName()).copy().withStyle(ChatFormatting.BOLD);
        g.drawString(font, name, x + 1, y, 0xFF000000 | col, true);
        String energy = (int) data.energy() + "/" + (int) god.maxEnergy();
        g.drawString(font, energy, x + WIDTH - 4 - font.width(energy), y, 0xFFB8ECFF, true);

        int bx = x + 1, by = y + 11, bw = WIDTH - 6;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 5, 0xFF02101C);
        int filled = (int) (bw * data.energy() / god.maxEnergy());
        g.fillGradient(bx, by, bx + filled, by + 4, 0xFF9FF0FF, 0xFF1E7FD8);
        if (avatar) {
            int left = (int) (bw * data.avatarTicks() / (float) com.thegods.god.AvatarManager.DURATION);
            g.fill(bx, by + 5, bx + left, by + 6, 0xFFFFD86B);
        }
        if (filled > 2) g.fill(bx + filled - 1, by, bx + filled, by + 4, 0xFFFFFFFF);

        // poteri
        int sel = Math.min(ClientGodState.selected, abilities.size() - 1);
        for (int i = 0; i < abilities.size(); i++) {
            Ability a = abilities.get(i);
            int ry = y + 22 + i * ROW;
            boolean selected = i == sel;
            int cd = data.cooldown(a.id());
            boolean affordable = data.energy() >= a.cost() || mc.player.isCreative();
            if (selected) {
                g.fill(x - 1, ry - 2, x + WIDTH - 2, ry + ROW - 3, 0x50000000 | col);
                g.fill(x - 1, ry - 2, x, ry + ROW - 3, 0xFFFFFFFF);
            }
            int nameColor = !active ? 0xFF6F8796 : a.ultimate() ? 0xFFFFD86B : cd > 0 || !affordable ? 0xFF8AA0AE : 0xFFEAF8FF;
            Component label = Component.translatable(a.nameKey());
            if (a.ultimate()) label = label.copy().withStyle(ChatFormatting.BOLD);
            g.drawString(font, (selected ? "› " : "  "), x + 1, ry, 0xFFFFFFFF, false);

            String right;
            int rightColor;
            if (cd > 0) {
                right = String.format("%.1fs", cd / 20f);
                rightColor = 0xFFFF7A7A;
                int max = (int) Math.max(1, a.cooldown() * GodsConfig.COOLDOWN_MULTIPLIER.get());
                int bar = (int) ((WIDTH - 12) * Math.min(1f, cd / (float) max));
                g.fill(x + 9, ry + 8, x + 9 + bar, ry + 9, 0x90FF7A7A);
            } else {
                right = (int) a.cost() + "✦";
                rightColor = affordable ? 0xFF7FE8FF : 0xFF52606A;
            }
            int rightX = x + WIDTH - 4 - font.width(right);
            var clipped = font.substrByWidth(label, rightX - (x + 9) - 4);
            g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(clipped), x + 9, ry, nameColor, true);
            g.drawString(font, right, rightX, ry, rightColor, true);
        }

        // suggerimenti tasti
        int hy = y + 24 + abilities.size() * ROW;
        boolean riding = mc.player.getVehicle() instanceof com.thegods.entity.WaterGolemEntity;
        Component hint = riding ? Component.translatable("hud.thegods.riding", ModKeys.USE_POWER.getTranslatedKeyMessage())
                : active
                ? Component.translatable("hud.thegods.hint", ModKeys.USE_POWER.getTranslatedKeyMessage(),
                ModKeys.PREV_POWER.getTranslatedKeyMessage(), ModKeys.NEXT_POWER.getTranslatedKeyMessage())
                : Component.translatable("hud.thegods.manifest_hint", ModKeys.MANIFEST.getTranslatedKeyMessage());
        g.pose().pushPose();
        g.pose().translate(x + 1, hy, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, hint, 0, 0, 0xFF9DB4C2, false);
        g.pose().popPose();
    }

    private GodHud() {}
}
