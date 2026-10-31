package com.thegods.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/thegods-client.toml */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FANCY_WATER;
    public static final ModConfigSpec.BooleanValue WATER_CAPE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Grafica").push("graphics");
        FANCY_WATER = b.comment("Usa lo shader dell'acqua divina (caustiche animate, riflessi).",
                        "Disattivalo se usi shaderpack (Iris/Oculus) e i modelli d'acqua non si vedono bene.")
                .define("fancyWaterShader", true);
        WATER_CAPE = b.comment("Mostra il mantello d'acqua con simulazione della stoffa")
                .define("waterCape", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
