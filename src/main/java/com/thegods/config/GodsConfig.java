package com.thegods.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/thegods-common.toml */
public final class GodsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue COOLDOWN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ENERGY_REGEN_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue DIVINE_FLIGHT;
    public static final ModConfigSpec.BooleanValue GIANT_FORM;
    public static final ModConfigSpec.BooleanValue ULTIMATE_CHANGES_WEATHER;
    public static final ModConfigSpec.BooleanValue PVP_DAMAGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Bilanciamento dei poteri divini").push("balance");
        DAMAGE_MULTIPLIER = b.comment("Moltiplicatore del danno di tutti i poteri")
                .defineInRange("damageMultiplier", 1.0, 0.0, 100.0);
        COOLDOWN_MULTIPLIER = b.comment("Moltiplicatore dei tempi di ricarica (0 = nessuna ricarica)")
                .defineInRange("cooldownMultiplier", 1.0, 0.0, 10.0);
        ENERGY_REGEN_MULTIPLIER = b.comment("Moltiplicatore della rigenerazione di energia divina")
                .defineInRange("energyRegenMultiplier", 1.0, 0.0, 100.0);
        PVP_DAMAGE = b.comment("I poteri possono colpire altri giocatori")
                .define("pvpDamage", true);
        b.pop();

        b.comment("Forma divina").push("form");
        DIVINE_FLIGHT = b.comment("La forma divina concede il volo")
                .define("divineFlight", true);
        GIANT_FORM = b.comment("La forma divina rende il giocatore più grande (attributo scale)")
                .define("giantForm", true);
        ULTIMATE_CHANGES_WEATHER = b.comment("Le ultimate possono cambiare il meteo (es. tempesta di Poseidone)")
                .define("ultimateChangesWeather", true);
        b.pop();
        SPEC = b.build();
    }

    public static float damage(float base) {
        return (float) (base * DAMAGE_MULTIPLIER.get());
    }

    private GodsConfig() {}
}
