package com.thegods.god;

/** Stato di un arto: carne, reciso, oppure costrutto divino (per Poseidone: acqua solida). */
public enum LimbState {
    FLESH,
    SEVERED,
    CONSTRUCT;

    public static LimbState of(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : FLESH;
    }

    public String nameKey() {
        return "limb_state.thegods." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
