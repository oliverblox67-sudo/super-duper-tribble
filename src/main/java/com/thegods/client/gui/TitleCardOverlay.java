package com.thegods.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Titolo cinematografico: banda scura, nome che appare lettera per lettera
 * con spaziatura larga, linee che si allungano e un sottotitolo in corsivo.
 */
public final class TitleCardOverlay {
    private static final int DURATION = 95;
    private static String titleKey = "", subtitleKey = "";
    private static int color = 0xFFFFFF;
    private static int age = -1;

    public static void show(String title, String subtitle, int rgb) {
        titleKey = title;
        subtitleKey = subtitle;
        color = rgb & 0xFFFFFF;
        age = 0;
    }

    public static void tick() {
        if (age >= 0 && ++age > DURATION) age = -1;
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (age < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;
        float t = age + delta.getGameTimeDeltaPartialTick(false);
        float in = Mth.clamp(t / 8f, 0, 1);
        float out = Mth.clamp((DURATION - t) / 16f, 0, 1);
        float alpha = Math.min(in, out);
        if (alpha <= 0.02f) return;

        int w = g.guiWidth(), h = g.guiHeight();
        int cy = h / 2 - 34;

        // banda
        int band = (int) (34 * ease(in));
        g.fillGradient(0, cy - band, w, cy, argb(0, 0), argb(alpha * 0.55f, 0x000814));
        g.fillGradient(0, cy, w, cy + band, argb(alpha * 0.55f, 0x000814), argb(0, 0));

        // titolo lettera per lettera
        String text = Component.translatable(titleKey).getString().toUpperCase(Locale.ROOT);
        float scale = 3.0f;
        int spacing = 3;
        int total = 0;
        for (int i = 0; i < text.length(); i++) total += font.width(String.valueOf(text.charAt(i))) + (i < text.length() - 1 ? spacing : 0);
        g.pose().pushPose();
        g.pose().translate(w / 2f, cy - 13, 0);
        g.pose().scale(scale, scale, 1);
        float x = -total / 2f;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float ca = alpha * Mth.clamp((t * 1.4f - i) / 3f, 0, 1);
            if (ca > 0.02f) {
                float rise = (1 - Mth.clamp((t * 1.4f - i) / 3f, 0, 1)) * 4;
                g.pose().pushPose();
                g.pose().translate(x, rise, 0);
                g.drawString(font, ch, 1, 1, argb(ca * 0.6f, darker(color)), false);
                g.drawString(font, ch, 0, 0, argb(ca, lighter(color)), false);
                g.pose().popPose();
            }
            x += font.width(ch) + spacing;
        }
        g.pose().popPose();

        // linee
        float lw = w * 0.32f * ease(Mth.clamp((t - 5) / 20f, 0, 1));
        int ly = cy + 15;
        g.fill((int) (w / 2f - lw), ly, (int) (w / 2f + lw), ly + 1, argb(alpha, color));
        g.fill((int) (w / 2f - lw * 0.6f), ly + 3, (int) (w / 2f + lw * 0.6f), ly + 4, argb(alpha * 0.5f, color));

        // sottotitolo
        float sa = alpha * Mth.clamp((t - 16) / 12f, 0, 1);
        if (sa > 0.02f && !subtitleKey.isEmpty()) {
            Component sub = Component.translatable(subtitleKey).withStyle(ChatFormatting.ITALIC);
            g.pose().pushPose();
            g.pose().translate(w / 2f, ly + 10, 0);
            g.pose().scale(1.3f, 1.3f, 1);
            g.drawString(font, sub, -font.width(sub) / 2, 0, argb(sa, 0xE6F7FF), true);
            g.pose().popPose();
        }
    }

    private static int argb(float a, int rgb) {
        int ai = Mth.clamp((int) (a * 255), 0, 255);
        if (ai < 5) ai = 5; // il font considera opaco un alpha quasi nullo
        return (ai << 24) | (rgb & 0xFFFFFF);
    }

    private static int lighter(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 255) + 90), g = Math.min(255, ((rgb >> 8) & 255) + 90), b = Math.min(255, (rgb & 255) + 90);
        return (r << 16) | (g << 8) | b;
    }

    private static int darker(int rgb) {
        return ((rgb >> 1) & 0x7F7F7F);
    }

    private static float ease(float x) {
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }

    private TitleCardOverlay() {}
}
