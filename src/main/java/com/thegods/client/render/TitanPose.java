package com.thegods.client.render;

import com.thegods.client.mesh.Skeleton;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Pose dello scheletro del Titano (mesh "titan").
 * Spazio del Titano: Y in alto, +Z avanti, braccio destro sul lato -X.
 * Convenzione: rotazione X negativa = l'arto va in avanti.
 */
public final class TitanPose {

    /** Animazione autonoma (Titano alleato). punch/slam = -1 se non attivi, altrimenti 0..1. */
    public record Input(float t, float walkPos, float walkSpeed, float punch, float slam, float headYaw, float headPitch) {}

    public static void apply(Skeleton s, Input in) {
        float swing = Mth.sin(in.walkPos() * 0.55f) * 0.75f * in.walkSpeed();
        float breathe = Mth.sin(in.t() * 0.09f) * 0.04f;
        s.translate("torso", 0, breathe, 0);

        float lean = 0;
        if (in.slam() >= 0) {
            float p = in.slam();
            lean = p < 0.55f ? -0.14f * (p / 0.55f) : Mth.lerp((p - 0.55f) / 0.45f, -0.14f, 0.5f) * (p > 0.85f ? (1 - p) / 0.15f : 1);
        }
        s.rotate("torso", lean, -swing * 0.12f, 0);
        s.rotate("head", Mth.clamp(in.headPitch(), -0.6f, 0.6f) - lean * 0.5f, -Mth.clamp(in.headYaw(), -1.0f, 1.0f), 0);

        // gambe
        s.rotate("thigh_r", -swing, 0, 0);
        s.rotate("thigh_l", swing, 0, 0);
        s.rotate("shin_r", Math.max(0, -swing) * 0.9f, 0, 0);
        s.rotate("shin_l", Math.max(0, swing) * 0.9f, 0, 0);

        // braccia (opposte alle gambe)
        float armR = swing * 0.8f, armL = -swing * 0.8f;
        float foreR = -0.3f, foreL = -0.3f;
        if (in.slam() >= 0) {
            float p = in.slam();
            float a = p < 0.55f ? Mth.lerp(p / 0.55f, 0, -3.0f) : Mth.lerp((p - 0.55f) / 0.45f, -3.0f, -0.6f);
            armR = armL = a;
            foreR = foreL = p < 0.55f ? -0.6f : -0.1f;
        } else if (in.punch() >= 0) {
            float p = in.punch();
            armR = p < 0.35f ? Mth.lerp(p / 0.35f, 0, -2.6f) : Mth.lerp(Math.min(1, (p - 0.35f) / 0.3f), -2.6f, -1.45f);
            foreR = p < 0.35f ? Mth.lerp(p / 0.35f, -0.3f, -1.4f) : Mth.lerp(Math.min(1, (p - 0.35f) / 0.3f), -1.4f, 0f);
        }
        s.rotate("arm_r", armR, 0, 0.1f);
        s.rotate("arm_l", armL, 0, -0.1f);
        s.rotate("forearm_r", foreR, 0, 0);
        s.rotate("forearm_l", foreL, 0, 0);
    }

    /**
     * Copia la posa del modello del giocatore (dopo setupAnim) sul Titano: l'Avatar si muove come te.
     * Spazio modello vanilla → spazio Titano: X uguale, Y e Z invertiti.
     */
    public static void fromPlayer(Skeleton s, PlayerModel<?> m, float t) {
        s.translate("torso", 0, Mth.sin(t * 0.09f) * 0.04f, 0);
        copy(s, "torso", m.body, 1f);
        copy(s, "head", m.head, 1f);
        copy(s, "arm_r", m.rightArm, 1f);
        copy(s, "arm_l", m.leftArm, 1f);
        copy(s, "thigh_r", m.rightLeg, 1f);
        copy(s, "thigh_l", m.leftLeg, 1f);
        // gomiti: piegati quando il braccio è alzato davanti, dritti a fine pugno
        s.rotate("forearm_r", -0.25f + Math.min(0, m.rightArm.xRot) * 0.35f, 0, 0);
        s.rotate("forearm_l", -0.25f + Math.min(0, m.leftArm.xRot) * 0.35f, 0, 0);
        // ginocchia: si piegano quando la gamba va indietro
        s.rotate("shin_r", Math.max(0, m.rightLeg.xRot) * 1.1f, 0, 0);
        s.rotate("shin_l", Math.max(0, m.leftLeg.xRot) * 1.1f, 0, 0);
    }

    private static void copy(Skeleton s, String bone, ModelPart part, float k) {
        s.rotate(bone, part.xRot * k, -part.yRot * k, -part.zRot * k);
    }

    private TitanPose() {}
}
