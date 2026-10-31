package com.thegods.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thegods.TheGods;
import com.thegods.client.render.AvatarRenderer;
import com.thegods.client.render.LimbConstructLayer;
import com.thegods.client.render.LimbConstructRender;
import com.thegods.god.GodData;
import com.thegods.god.Limb;
import com.thegods.god.LimbState;
import com.thegods.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Nasconde gli arti recisi e disegna i costrutti anche in prima persona. */
@EventBusSubscriber(modid = TheGods.MODID, value = Dist.CLIENT)
public final class LimbRenderEvents {

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onRenderAvatar(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer player
                && AvatarRenderer.render(player, event.getRenderer(), event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), event.getPartialTick())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (!(Minecraft.getInstance().player instanceof AbstractClientPlayer player)) return;
        if (AvatarRenderer.grow(player, event.getPartialTick()) < 0.5f) return;
        event.setCanceled(true);
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            AvatarRenderer.renderHands(player, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        GodData data = event.getEntity().getData(ModAttachments.GOD_DATA);
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        for (Limb limb : Limb.ALL) {
            if (data.limb(limb) == LimbState.FLESH) continue;
            LimbConstructLayer.part(model, limb).visible = false;
            LimbConstructLayer.overlay(model, limb).visible = false;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        for (Limb limb : Limb.ALL) LimbConstructLayer.part(model, limb).visible = true;
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        GodData data = player.getData(ModAttachments.GOD_DATA);
        Limb limb = event.getArm() == HumanoidArm.RIGHT ? Limb.RIGHT_ARM : Limb.LEFT_ARM;
        LimbState state = data.limb(limb);
        if (state == LimbState.FLESH) return;
        event.setCanceled(true);
        if (state != LimbState.CONSTRUCT) return; // reciso: niente braccio

        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.attackTime = 0;
        model.crouching = false;
        model.swimAmount = 0;
        model.setupAnim(player, 0, 0, 0, 0, 0);
        ModelPart part = LimbConstructLayer.part(model, limb);
        part.xRot = 0;
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float form = Mth.lerp(pt, data.clientLimbFormPrev[limb.ordinal()], data.clientLimbForm[limb.ordinal()]);
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        part.translateAndRotate(ps);
        LimbConstructRender.render(ps, event.getMultiBufferSource(), limb, form, player.tickCount + pt,
                player.getSkin().model() == PlayerSkin.Model.SLIM);
        ps.popPose();
    }

    private LimbRenderEvents() {}
}
