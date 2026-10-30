package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thegods.god.GodData;
import com.thegods.god.Limb;
import com.thegods.god.LimbState;
import com.thegods.registry.ModAttachments;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;

/** Disegna gli arti-costrutto al posto di quelli recisi (terza persona e anteprima GUI). */
public class LimbConstructLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public LimbConstructLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        GodData data = player.getData(ModAttachments.GOD_DATA);
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        for (Limb limb : Limb.ALL) {
            if (data.limb(limb) != LimbState.CONSTRUCT) continue;
            float form = Mth.lerp(partialTick, data.clientLimbFormPrev[limb.ordinal()], data.clientLimbForm[limb.ordinal()]);
            ModelPart part = part(getParentModel(), limb);
            ps.pushPose();
            part.translateAndRotate(ps);
            LimbConstructRender.render(ps, buffers, limb, form, ageInTicks, slim);
            ps.popPose();
        }
    }

    public static ModelPart part(PlayerModel<?> model, Limb limb) {
        return switch (limb) {
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
        };
    }

    public static ModelPart overlay(PlayerModel<?> model, Limb limb) {
        return switch (limb) {
            case RIGHT_ARM -> model.rightSleeve;
            case LEFT_ARM -> model.leftSleeve;
            case RIGHT_LEG -> model.rightPants;
            case LEFT_LEG -> model.leftPants;
        };
    }
}
