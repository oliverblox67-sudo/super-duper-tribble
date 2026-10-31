package com.thegods.client;

import com.thegods.TheGods;
import com.thegods.client.gui.TitleCardOverlay;
import com.thegods.god.God;
import com.thegods.god.GodData;
import com.thegods.god.Gods;
import com.thegods.god.Limb;
import com.thegods.god.LimbManager;
import com.thegods.god.LimbState;
import com.thegods.client.gui.BodyForgeScreen;
import com.thegods.network.ManifestPayload;
import com.thegods.network.UseAbilityPayload;
import com.thegods.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TheGods.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        CameraShake.tick();
        TitleCardOverlay.tick();
        if (ShowcaseDirector.ENABLED) ShowcaseDirector.tick(mc);
        if (mc.level == null || mc.player == null) return;

        // Animazioni della forma per tutti i giocatori visibili
        for (AbstractClientPlayer p : mc.level.players()) {
            GodData d = p.getData(ModAttachments.GOD_DATA);
            d.clientFormPrev = d.clientForm;
            d.clientUltimatePrev = d.clientUltimate;
            d.clientForm = approach(d.clientForm, d.isManifested() ? 1 : 0, 1 / 22f);
            d.clientUltimate = approach(d.clientUltimate, d.isManifested() && d.ultimateTicks() > 0 ? 1 : 0, 1 / 15f);
            d.clientAvatarPrev = d.clientAvatar;
            d.clientAvatar = approach(d.clientAvatar, d.avatarGrow(), 1 / 14f);
            if (d.clientAvatar > 0.6f && p.getRandom().nextInt(2) == 0) avatarAmbient(p);
            if (d.clientForm > 0.5f && d.is(Gods.POSEIDON)) poseidonAmbient(p, d);
            if (d.clientForm > 0.01f && d.is(Gods.POSEIDON)) {
                boolean airborne = p.getAbilities().flying || (!p.onGround() && !p.isInWater());
                com.thegods.client.render.WingPhysics.tick(p, p.tickCount, airborne);
                if (ClientConfig.WATER_CAPE.get() && d.clientAvatar < 0.1f) com.thegods.client.render.WaterCape.tick(p);
            }
            for (Limb limb : Limb.ALL) {
                int i = limb.ordinal();
                d.clientLimbFormPrev[i] = d.clientLimbForm[i];
                d.clientLimbForm[i] = d.limb(limb) == LimbState.CONSTRUCT ? Math.min(1, d.clientLimbForm[i] + 1 / 30f) : 0;
                if (d.limb(limb) == LimbState.CONSTRUCT && p.getRandom().nextInt(25) == 0) {
                    Vec3 at = LimbManager.limbPos(p, limb);
                    p.level().addParticle(ParticleTypes.DRIPPING_WATER, at.x, at.y - 0.2, at.z, 0, 0, 0);
                }
            }
        }

        if (mc.player.tickCount % 100 == 0) {
            com.thegods.client.render.WingPhysics.cleanup(mc.player.tickCount);
            com.thegods.client.render.WaterCape.cleanup(mc.player.tickCount);
        }
        handleKeys(mc);
    }

    private static void handleKeys(Minecraft mc) {
        GodData data = mc.player.getData(ModAttachments.GOD_DATA);
        God god = data.god();
        while (ModKeys.BODY_FORGE.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new BodyForgeScreen());
        }
        while (ModKeys.MANIFEST.consumeClick()) {
            if (god != null) PacketDistributor.sendToServer(ManifestPayload.INSTANCE);
        }
        boolean avatar = data.isAvatar();
        if (avatar != ClientGodState.lastAvatar) {
            ClientGodState.lastAvatar = avatar;
            ClientGodState.selected = 0;
        }
        int count = god == null ? 0 : god.activeAbilities(data).size();
        if (count > 0) ClientGodState.selected = Mth.clamp(ClientGodState.selected, 0, count - 1);
        while (ModKeys.NEXT_POWER.consumeClick()) {
            if (count == 0) continue;
            ClientGodState.selected = (ClientGodState.selected + 1) % count;
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.6f);
        }
        while (ModKeys.PREV_POWER.consumeClick()) {
            if (count == 0) continue;
            ClientGodState.selected = (ClientGodState.selected - 1 + count) % count;
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.4f);
        }
        while (ModKeys.USE_POWER.consumeClick()) {
            if (count > 0) PacketDistributor.sendToServer(new UseAbilityPayload(ClientGodState.selected));
        }
    }

    /** Acqua che cola dal titano. */
    private static void avatarAmbient(AbstractClientPlayer p) {
        RandomSource r = p.getRandom();
        double w = p.getBbWidth() * 0.9, h = p.getBbHeight();
        p.level().addParticle(ParticleTypes.FALLING_WATER, p.getX() + (r.nextDouble() - 0.5) * w * 2, p.getY() + r.nextDouble() * h,
                p.getZ() + (r.nextDouble() - 0.5) * w * 2, 0, 0, 0);
        if (r.nextInt(3) == 0) p.level().addParticle(ParticleTypes.BUBBLE_POP, p.getX() + (r.nextDouble() - 0.5) * w,
                p.getY() + h * 0.6, p.getZ() + (r.nextDouble() - 0.5) * w, 0, 0.05, 0);
    }

    /** Gocce che cadono dalle ali e bolle attorno a Poseidone. */
    private static void poseidonAmbient(AbstractClientPlayer p, GodData d) {
        RandomSource r = p.getRandom();
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        int n = d.ultimateTicks() > 0 ? 6 : 2;
        for (int i = 0; i < n; i++) {
            double s = (r.nextDouble() - 0.5) * 6.5;
            Vec3 pos = p.position().add(back.scale(0.6 + r.nextDouble() * 0.8)).add(side.scale(s))
                    .add(0, 1.2 + r.nextDouble() * 2.2 - Math.abs(s) * 0.25, 0);
            p.level().addParticle(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 0, 0, 0);
        }
        if (r.nextInt(4) == 0) {
            p.level().addParticle(ParticleTypes.BUBBLE_POP, p.getX() + r.nextGaussian() * 0.6, p.getY() + 1 + r.nextGaussian() * 0.6,
                    p.getZ() + r.nextGaussian() * 0.6, 0, 0.04, 0);
        }
    }

    /** Da Avatar la telecamera in terza persona sta più vicina: si vede il pilota dentro il titano. */
    @SubscribeEvent
    public static void onCameraDistance(net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent event) {
        if (event.getCamera().getEntity() instanceof AbstractClientPlayer p) {
            float grow = p.getData(ModAttachments.GOD_DATA).clientAvatar;
            if (grow > 0) event.setDistance(event.getDistance() * Mth.lerp(grow, 1f, 0.62f));
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float s = CameraShake.current((float) event.getPartialTick());
        if (s <= 0) return;
        double time = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + event.getPartialTick();
        event.setYaw(event.getYaw() + (float) (Math.sin(time * 1.7) * 1.4 * s));
        event.setPitch(event.getPitch() + (float) (Math.sin(time * 2.3 + 1) * 1.2 * s));
        event.setRoll(event.getRoll() + (float) (Math.sin(time * 1.3 + 2) * 2.0 * s));
    }

    private static float approach(float v, float target, float step) {
        return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
    }

    private ClientEvents() {}
}
